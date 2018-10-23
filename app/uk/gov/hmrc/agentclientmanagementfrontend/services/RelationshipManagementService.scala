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
import uk.gov.hmrc.agentclientmanagementfrontend.connectors.{AgentClientRelationshipsConnector, AgentServicesAccountConnector, PirRelationshipConnector}
import uk.gov.hmrc.agentclientmanagementfrontend.models._
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, InvitationId, MtdItId, Vrn}
import uk.gov.hmrc.domain.{Nino, TaxIdentifier}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

case class DeleteResponse(response: Boolean, agencyName: String, service: String)

class RelationshipManagementService @Inject()(pirRelationshipConnector: PirRelationshipConnector,
                                              agentServicesAccountConnector: AgentServicesAccountConnector,
                                              relationshipsConnector: AgentClientRelationshipsConnector,
                                              sessionStoreService: SessionStoreService) {

  def getAuthorisedAgents(clientIdOpt: OptionalClientIdentifiers)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Seq[AuthorisedAgent]] = {
    val pirRelationships = relationships(clientIdOpt.nino) { case nino: Nino => pirRelationshipConnector.getClientRelationships(removeNinoSpaces(nino)) }
    val itsaRelationships = relationships(clientIdOpt.mtdItId)(_ => relationshipsConnector.getActiveClientItsaRelationship.map(_.toSeq))
    val vatRelationships = relationships(clientIdOpt.vrn)(_ => relationshipsConnector.getActiveClientVatRelationship.map(_.toSeq))
    val relationshipWithAgencyNames = for {
      relationships <- Future.sequence(Seq(itsaRelationships, pirRelationships, vatRelationships)).map(_.flatten)
      agencyNames <- if (relationships.nonEmpty)
        agentServicesAccountConnector.getAgencyNames(relationships.map(_.arn))
      else Future.successful(Map.empty[Arn, String])
    } yield (relationships, agencyNames)

    relationshipWithAgencyNames.flatMap {
      case (relationships, agencyNames) =>
        def uuId = UUID.randomUUID().toString.replace("-", "")
        val relationshipWithArnCache = relationships.map(r =>
          ClientCache(uuId, r.arn, agencyNames.getOrElse(r.arn, ""), r.serviceName, r.dateFrom))

        sessionStoreService.storeClientCache(relationshipWithArnCache).map { _ =>
          relationshipWithArnCache.map { case cache =>
            AuthorisedAgent(cache.uuId, cache.service, cache.agencyName, cache.dateAuthorised)
          }.sortWith(_.agencyName.toLowerCase < _.agencyName.toLowerCase).sorted(AuthorisedAgent.orderingByDateFrom)
        }
    }
  }

  def deleteITSARelationship(id: String, clientId: MtdItId)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[DeleteResponse] = {
    deleteRelationship(id, clientId)(arn => relationshipsConnector.deleteItsaRelationship(arn, clientId))
  }

  def deletePIRelationship(id: String, nino: Nino)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[DeleteResponse] = {
    deleteRelationship(id, nino)(arn => pirRelationshipConnector.deleteClientRelationship(arn, nino))
  }

  def deleteVATRelationship(id: String, vrn: Vrn)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[DeleteResponse] = {
    deleteRelationship(id, vrn)(arn => relationshipsConnector.deleteVatRelationship(arn, vrn))
  }

  private def deleteRelationship(id: String, clientId: TaxIdentifier)(f: Arn => Future[Boolean])(implicit c: HeaderCarrier, ec: ExecutionContext): Future[DeleteResponse] = {
    for {
      clientCacheOpt: Option[Seq[ClientCache]] <- sessionStoreService.fetchClientCache
      clientCache = clientCacheOpt.flatMap(_.find(_.uuId == id))

      deleteResponse <- clientCache match {
        case Some(cache) =>
          val remainingCache: Seq[ClientCache] = clientCacheOpt.get.filterNot(_ == cache)
          for {
            deletion <- f(cache.arn).andThen { case Success(true) =>
                    for {
                      _ <- sessionStoreService.remove()
                      _ <- sessionStoreService.storeClientCache(remainingCache)
                    } yield ()
                }
              .map(DeleteResponse(_, cache.agencyName, cache.service))
          } yield deletion

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

  def relationships(identifierOpt: Option[TaxIdentifier])(f: TaxIdentifier => Future[Seq[Relationship]]) = identifierOpt match {
    case Some(identifier) => f(identifier)
    case None => Future.successful(Seq.empty)
  }

  def removeNinoSpaces(nino: Nino): Nino = {
    Nino(nino.value.replace(" ", ""))
  }
}