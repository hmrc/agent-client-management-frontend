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
import uk.gov.hmrc.agentclientmanagementfrontend.models.{ClientCache, AuthorisedAgent}
import uk.gov.hmrc.agentclientmanagementfrontend.util.Services
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

case class DeleteResponse(response: Boolean, agencyName: String, service: String)

class RelationshipManagementService @Inject()(pirRelationshipConnector: PirRelationshipConnector,
                                              desConnector: DesConnector,
                                              agentServicesAccountConnector: AgentServicesAccountConnector,
                                              agentClientRelationshipsConnector: AgentClientRelationshipsConnector,
                                              sessionStoreService: SessionStoreService) {

  def getAuthorisedAgents(clientId: MtdItId)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Seq[AuthorisedAgent]] = {
    val relationshipWithAgencyNames = for {
      nino <- desConnector.getNinoFor(clientId)
      pir <- pirRelationshipConnector.getClientRelationships(nino)
      itsa <- desConnector.getActiveClientItsaRelationships(clientId).map(_.toSeq)
      relationships = itsa ++ pir
      agencyNames <- if (relationships.nonEmpty)
        agentServicesAccountConnector.getAgencyNames(relationships.map(_.arn))
      else Future.successful(Map.empty[Arn, String])
    } yield (relationships, agencyNames, nino)

    relationshipWithAgencyNames.flatMap {
      case (relationships, agencyNames, nino) =>
        def uuId = UUID.randomUUID().toString.replace("-", "")
        val relationshipWithArnCache = relationships.map(r =>
          ClientCache(uuId, r.arn, nino, agencyNames.getOrElse(r.arn, ""), r.serviceName))

        sessionStoreService.storeClientCache(relationshipWithArnCache).map { _ =>
          relationshipWithArnCache.map { case cache =>
            AuthorisedAgent(cache.uuId, cache.service, cache.agencyName)
          }
        }
    }
  }

  def deleteRelationship(id: String, clientId: MtdItId)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[DeleteResponse] = {
    for {
      clientCacheOpt <- sessionStoreService.fetchClientCache
      clientCache = clientCacheOpt.flatMap(_.find(_.uuId == id))
      deleteResponse <- clientCache match {
        case Some(cache) => deleteAgentClientRelationshipFor(cache.arn, clientId, cache.nino, cache.service)
          .map(DeleteResponse(_, cache.agencyName, cache.service))
        case None => Future.failed(new Exception("failed to retrieve session cache"))
      }
    } yield deleteResponse
  }

  def getAuthorisedAgentDetails(id: String)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[(String, String)]] = {
    for {
      cacheOpt <- sessionStoreService.fetchClientCache
      cache = cacheOpt.flatMap(_.find(_.uuId == id))
    } yield cache.map(cache => (cache.agencyName, cache.service))
  }

  private def deleteAgentClientRelationshipFor(arn: Arn, clientId: MtdItId, nino: Nino, service: String)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    service match {
      case Services.ITSA => agentClientRelationshipsConnector.deleteRelationship(arn, clientId)
      case Services.HMRCPIR => pirRelationshipConnector.deleteClientRelationship(arn, nino)
    }
  }
}