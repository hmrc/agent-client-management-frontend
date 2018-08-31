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

import javax.inject.Inject
import org.joda.time.LocalDate
import uk.gov.hmrc.agentclientmanagementfrontend.connectors.{AgentClientAuthorisationConnector, AgentServicesAccountConnector}
import uk.gov.hmrc.agentclientmanagementfrontend.models._
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, Vrn}
import uk.gov.hmrc.domain.{Nino, TaxIdentifier}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class AgentClientAuthorisationService @Inject()(agentClientAuthorisationConnector: AgentClientAuthorisationConnector, agentServicesAccountConnector: AgentServicesAccountConnector) {

  def getAgentRequests(clientIdOpt: OptionalClientIdentifiers)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[AgentRequest]] = {

    val storedItsaInvitations = invitations(clientIdOpt.mtdItId)(clientId => agentClientAuthorisationConnector.getItsaInvitation(MtdItId(clientId.value)))
    val storedIrvInvitations = invitations(clientIdOpt.nino)(clientId => agentClientAuthorisationConnector.getIrvInvitation(Nino(clientId.value)))
    val storedVatInvitations = invitations(clientIdOpt.vrn)(clientId => agentClientAuthorisationConnector.getVatInvitation(Vrn(clientId.value)))
    val relationshipsWithAgencyNamesWithStoredInvitations = for {
      storedInvitations <- Future.sequence(Seq(storedItsaInvitations, storedIrvInvitations, storedVatInvitations)).map(_.flatten)
      agencyNames <- if(storedInvitations.nonEmpty)
        agentServicesAccountConnector.getAgencyNames(storedInvitations.map(_.arn).distinct)
      else Future.successful(Map.empty[Arn, String])
    } yield (agencyNames, storedInvitations)

    relationshipsWithAgencyNamesWithStoredInvitations.map {
      case (agencyNames, storedInvites) =>
        storedInvites.map(si =>
         AgentRequest(si.service, agencyNames.getOrElse(si.arn, ""), si.status, si.expiryDate, si.lastUpdated.toLocalDate, si.invitationId, getSortedDate(si.status, si.expiryDate, si.lastUpdated.toLocalDate))
        ).sorted(AgentRequest.orderingByAgencyName).sorted(AgentRequest.orderingBySortDate)
    }
  }

  def getSortedDate (status: String, expiryDate: LocalDate, lastUpdated: LocalDate): LocalDate = {
    implicit val now: LocalDate = LocalDate.now()
    def effectiveStatus(implicit now: LocalDate): String =
      if (status == "Pending" && (now.isAfter(expiryDate) || now.isEqual(expiryDate))) "Expired"
      else status
    if(effectiveStatus == "Expired") expiryDate else lastUpdated
  }

  def invitations(identifierOpt: Option[TaxIdentifier])(f: TaxIdentifier => Future[Seq[StoredInvitation]]) = identifierOpt match {
    case Some(identifier) => f(identifier)
    case None => Future.successful(Seq.empty)
  }
}