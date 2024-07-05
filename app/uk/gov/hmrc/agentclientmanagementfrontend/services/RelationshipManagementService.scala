/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.Logging
import uk.gov.hmrc.agentclientmanagementfrontend.connectors.{AgentClientAuthorisationConnector, AgentClientRelationshipsConnector, PirRelationshipConnector}
import uk.gov.hmrc.agentclientmanagementfrontend.models._
import uk.gov.hmrc.agentmtdidentifiers.model.Service.{HMRCCBCNONUKORG, HMRCCBCORG}
import uk.gov.hmrc.agentmtdidentifiers.model._
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.domain.{Nino, TaxIdentifier}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

case class DeleteResponse(response: Boolean, agencyName: String, service: String)

class RelationshipManagementService @Inject() (
  pirRelationshipConnector: PirRelationshipConnector,
  acaConnector: AgentClientAuthorisationConnector,
  relationshipsConnector: AgentClientRelationshipsConnector,
  sessionStoreService: MongoDBSessionStoreService
) extends Logging {

  implicit val localDateOrdering: Ordering[LocalDateTime] = _ compareTo _

  def getAuthorisedAgents(clientIdOpt: ClientIdentifiers)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Seq[AuthorisedAgent]] = {
    val pirRelationships = relationships(clientIdOpt.nino) { case nino: Nino =>
      pirRelationshipConnector.getClientRelationships(removeNinoSpaces(nino))
    }
    val itsaRelationships =
      relationships(clientIdOpt.mtdItId)(_ => relationshipsConnector.getActiveClientItsaRelationship.map(_.toSeq))
    val altItsaRelationships =
      clientIdOpt.nino
        .map(nino =>
          acaConnector
            .getInvitation(nino, ninoForItsa = true)
            .map(_.filter(_.status == "Partialauth").map(AltItsaRelationship.fromStoredInvitation))
        )
        .getOrElse(Future successful Seq.empty)
    val vatRelationships =
      relationships(clientIdOpt.vrn)(_ => relationshipsConnector.getActiveClientVatRelationship.map(_.toSeq))
    val trustRelationships =
      relationships(clientIdOpt.utr)(_ => relationshipsConnector.getActiveClientTrustRelationship.map(_.toSeq))
    val cgtRelationships =
      relationships(clientIdOpt.cgtRef)(_ => relationshipsConnector.getActiveClientCgtRelationship.map(_.toSeq))
    val urnRelationships =
      relationships(clientIdOpt.urn)(_ => relationshipsConnector.getActiveClientTrustNtRelationship.map(_.toSeq))
    val pptRelationships =
      relationships(clientIdOpt.pptRef)(_ => relationshipsConnector.getActiveClientPptRelationship.map(_.toSeq))
    val cbcUKRelationships =
      relationships(clientIdOpt.cbcUkRef)(_ => relationshipsConnector.getActiveClientCbcRelationship(isUkUser = true).map(_.toSeq))
    val cbcNonUKRelationships =
      relationships(clientIdOpt.cbcNonUkRef)(_ => relationshipsConnector.getActiveClientCbcRelationship(isUkUser = false).map(_.toSeq))
    val plrRelationships =
      relationships(clientIdOpt.plrId)(_ => relationshipsConnector.getActiveClientPlrRelationship.map(_.toSeq))

    val relationshipWithAgencyNames = for {
      relationships <- Future
                         .sequence(
                           Seq(
                             itsaRelationships,
                             altItsaRelationships,
                             pirRelationships,
                             vatRelationships,
                             trustRelationships,
                             urnRelationships,
                             cgtRelationships,
                             pptRelationships,
                             cbcUKRelationships,
                             cbcNonUKRelationships,
                             plrRelationships
                           )
                         )
                         .map(_.flatten)
      agencyNames <- if (relationships.nonEmpty)
                       acaConnector.getAgencyNames(relationships.map(_.arn))
                     else Future.successful(Map.empty[Arn, String])
    } yield (relationships, agencyNames)

    relationshipWithAgencyNames.flatMap { case (relationships, agencyNames) =>
      def uuId: String = UUID.randomUUID().toString.replace("-", "")

      val relationshipWithArnCache =
        relationships.map(r => ClientCache(uuId, r.arn, agencyNames.getOrElse(r.arn, ""), r.serviceName, r.dateFrom, r.isAltItsa))

      sessionStoreService.storeClientCache(relationshipWithArnCache).map { _ =>
        relationshipWithArnCache
          .map { cache =>
            AuthorisedAgent(cache.uuId, cache.service, cache.agencyName, cache.dateAuthorised)
          }
          .sortWith(_.agencyName.toLowerCase < _.agencyName.toLowerCase)
          .sorted(AuthorisedAgent.orderingByDateFrom)
      }
    }
  }

  def getDeAuthorisedAgents(
    clientIdOpt: ClientIdentifiers
  )(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Seq[Inactive]] = {
    val pirInactiveRelationships = inactiveRelationships(clientIdOpt.nino) { case _: Nino =>
      pirRelationshipConnector.getInactiveClientRelationships()
    }
    val otherInactiveRelationships =
      if (clientIdOpt.hasOnlyNino) Future successful (Seq.empty)
      else relationshipsConnector.getInactiveClientRelationships
    for {
      pir   <- pirInactiveRelationships
      other <- otherInactiveRelationships
    } yield pir ++ other
  }

  /*
  The Deauthorised status was introduced in 2020(?) and since MYTA does not have a time limit in the History tab,
  we can't rely solely on this status to determine deauthorised relationships.
   */
  def matchAndRefineStatus(agentRequests: Seq[AgentRequest], inactive: Seq[Inactive]): Seq[AgentRequest] = {

    val Accepted = "Accepted"
    val Deauthorised = "Deauthorised"

    def acceptedOrDeauthorised(ar: AgentRequest) = ar.status == Accepted || ar.status == Deauthorised

    def normalisedServiceName(service: String): String =
      if (service == HMRCCBCNONUKORG) HMRCCBCORG else service

    agentRequests
      .filter(acceptedOrDeauthorised)
      .groupBy(_.arn)
      .flatMap { case (arn, agentRequests) =>
        agentRequests
          .groupBy(_.serviceName)
          .flatMap { case (service, agentRequests) =>
            agentRequests
              .sortBy(_.lastUpdated)
              .map(Option.apply)
              .zipAll(
                inactive
                  .filter(x => x.arn == arn && x.serviceName == normalisedServiceName(service))
                  .map(Option.apply),
                None,
                None
              )
              .flatMap {
                case (Some(ar), Some(in)) =>
                  Option.apply(
                    ar
                      .copy(
                        status = s"AcceptedThenCancelledBy${ar.relationshipEndedBy.getOrElse("Agent")}",
                        lastUpdated = in.dateTo.fold(ar.lastUpdated)(_.atStartOfDay())
                      )
                  )
                case (Some(ar), None) if ar.status == Deauthorised =>
                  Option.apply(
                    ar
                      .copy(
                        status = s"AcceptedThenCancelledBy${ar.relationshipEndedBy.getOrElse("Agent")}",
                        lastUpdated = ar.lastUpdated
                      )
                  )
                case (Some(ar), None) => Option.apply(ar)
                case _ =>
                  logger.warn("inactive record not matched with an authorisation request!")
                  None
              }
          }
      }
      .toSeq ++ agentRequests.filterNot(acceptedOrDeauthorised)
  }

  def deleteRelationship(id: String, clientIdentifiers: ClientIdentifiers, service: String)(implicit
    c: HeaderCarrier,
    ec: ExecutionContext
  ): Future[DeleteResponse] =
    clientIdentifiers.getIdentifierForService(service) match {
      case None           => throw new InsufficientEnrolments
      case Some(clientId) => deleteRelationship(id)(arn => relationshipsConnector.deleteRelationship(arn, clientId, service))
    }

  def deleteAltItsaRelationship(id: String, clientId: Nino)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[DeleteResponse] =
    deleteRelationship(id)(arn => acaConnector.setRelationshipEnded(arn, clientId))

  def deletePIRelationship(id: String, nino: Nino)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[DeleteResponse] =
    deleteRelationship(id)(arn => pirRelationshipConnector.deleteClientRelationship(arn, nino))

  private def deleteRelationship(id: String)(f: Arn => Future[Boolean])(implicit c: HeaderCarrier, ec: ExecutionContext): Future[DeleteResponse] =
    for {
      clientCacheOpt: Option[Seq[ClientCache]] <- sessionStoreService.fetchClientCache
      clientCache = clientCacheOpt.flatMap(_.find(_.uuId == id))

      deleteResponse <- clientCache match {
                          case Some(cache) =>
                            val remainingCache: Seq[ClientCache] = clientCacheOpt.get.filterNot(_ == cache)
                            for {
                              deletion <- f(cache.arn)
                                            .andThen { case Success(true) =>
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

  def getAuthorisedAgentDetails(id: String)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[(String, String, Boolean)]] =
    for {
      cacheOpt <- sessionStoreService.fetchClientCache
      cache = cacheOpt.flatMap(_.find(_.uuId == id))
    } yield cache.map(cache => (cache.agencyName, cache.service, cache.isAltItsa))

  def relationships(identifierOpt: Option[TaxIdentifier])(f: TaxIdentifier => Future[Seq[Relationship]]): Future[Seq[Relationship]] =
    identifierOpt match {
      case Some(identifier) => f(identifier)
      case None             => Future.successful(Seq.empty)
    }

  def inactiveRelationships(
    identifierOpt: Option[TaxIdentifier]
  )(f: TaxIdentifier => Future[Seq[PirInactiveRelationship]]): Future[Seq[PirInactiveRelationship]] =
    identifierOpt match {
      case Some(identifier) => f(identifier)
      case None             => Future.successful(Seq.empty)
    }

  def removeNinoSpaces(nino: Nino): Nino =
    Nino(nino.value.replace(" ", ""))
}
