/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentclientmanagementfrontend.services

import java.util.UUID
import javax.inject.Inject

import uk.gov.hmrc.agentclientmanagementfrontend.connectors.{AgentClientRelationshipsConnector, AgentServicesAccountConnector, DesConnector, PirRelationshipConnector}
import uk.gov.hmrc.agentclientmanagementfrontend.models.{ArnCache, AuthorisedAgent}
import uk.gov.hmrc.agentclientmanagementfrontend.util.Services
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class RelationshipManagementService @Inject()(pirRelationshipConnector: PirRelationshipConnector,
                                              desConnector: DesConnector,
                                              agentServicesAccountConnector: AgentServicesAccountConnector,
                                              agentClientRelationshipsConnector: AgentClientRelationshipsConnector,
                                              sessionStoreService: SessionStoreService) {

  def getAuthorisedAgents(clientId: MtdItId)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Seq[AuthorisedAgent]] = {
    val relationshipWithAgencyNames = for {
      pir <- pirRelationshipConnector.getClientRelationships(clientId)
      itsa <- desConnector.getActiveClientItsaRelationships(clientId).map(_.toSeq)
      relationships = itsa ++ pir
      agencyNames <- if (relationships.nonEmpty)
        agentServicesAccountConnector.getAgencyNames(relationships.map(_.arn))
      else Future.successful(Map.empty[Arn, String])
    } yield (relationships, agencyNames)

    relationshipWithAgencyNames.flatMap {
      case (relationships, agencyNames) =>
        def uuId = UUID.randomUUID().toString.replace("-", "")
        val relationshipWithArnCache = relationships.map(r =>
          ArnCache(uuId, r.arn, agencyNames.getOrElse(r.arn, ""), r.serviceName))

        sessionStoreService.storeArnCache(relationshipWithArnCache).map { _ =>
          relationshipWithArnCache.map { case arnCache =>
            AuthorisedAgent(arnCache.uuId, arnCache.service, arnCache.agencyName)
          }
        }
    }
  }

  def deleteRelationship(id: String, clientId: MtdItId)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    for {
      arnCacheOpt <- sessionStoreService.fetchArnCache
      arnCache = arnCacheOpt.flatMap(_.find(_.uuId == id))
      deleteResponse <- arnCache match {
        case Some(cache) => deleteAgentClientRelationshipFor(cache.arn, clientId, cache.service)
        case None => Future.failed(new Exception("failed to retrieve cache")) //TODO
      }
    } yield deleteResponse
  }

  def getAuthorisedAgentDetails(id: String)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[(String, String)]] = {
    for {
      arnCacheOpt <- sessionStoreService.fetchArnCache
      arnCache = arnCacheOpt.flatMap(_.find(_.uuId == id))
    } yield arnCache.map(cache => (cache.agencyName, cache.service))
  }

  private def deleteAgentClientRelationshipFor(arn: Arn, clientId: MtdItId, service: String)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    service match {
      case Services.ITSA => agentClientRelationshipsConnector.deleteRelationship(arn, clientId)
      case Services.HMRCPIR => pirRelationshipConnector.deleteClientRelationship(arn, clientId)
    }
  }
}