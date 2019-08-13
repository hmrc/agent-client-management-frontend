/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.agentclientmanagementfrontend.connectors.{AgentClientAuthorisationConnector, AgentServicesAccountConnector}
import uk.gov.hmrc.agentclientmanagementfrontend.models._
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, Utr, Vrn}
import uk.gov.hmrc.domain.{Nino, TaxIdentifier}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class AgentClientAuthorisationService @Inject()(agentClientAuthorisationConnector: AgentClientAuthorisationConnector, agentServicesAccountConnector: AgentServicesAccountConnector, relationshipManagementService: RelationshipManagementService) {

  def getAgentRequests(clientType: String, clientIdOpt: ClientIdentifiers)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[AgentRequest]] = {

    val storedItsaInvitations = invitations(clientIdOpt.mtdItId)(clientId => agentClientAuthorisationConnector.getItsaInvitation(MtdItId(clientId.value)))
    val storedIrvInvitations = invitations(clientIdOpt.nino)(clientId => agentClientAuthorisationConnector.getIrvInvitation(relationshipManagementService.removeNinoSpaces(Nino(clientId.value))))
    val storedVatInvitations = invitations(clientIdOpt.vrn)(clientId => agentClientAuthorisationConnector.getVatInvitation(Vrn(clientId.value)))
    val storedTrustInvitations = invitations(clientIdOpt.utr)(clientId => agentClientAuthorisationConnector.getTrustInvitation(Utr(clientId.value)))

    val relationshipsWithAgencyNamesWithStoredInvitations = for {
      storedInvitations <- Future.sequence(Seq(storedItsaInvitations, storedIrvInvitations, storedVatInvitations, storedTrustInvitations)).map(_.flatten)
      agencyNames <- if(storedInvitations.nonEmpty)
          agentServicesAccountConnector.getAgencyNames(storedInvitations.map(_.arn).distinct)
      else Future.successful(Map.empty[Arn, String])
      agentRefs <- agentClientAuthorisationConnector
        .getAgentReferences(storedInvitations
          .filter(_.status == "Pending")
          .map(_.arn))
    } yield (agencyNames, storedInvitations, agentRefs)

    relationshipsWithAgencyNamesWithStoredInvitations.map {
      case (agencyName, storedInvites, agentRef) =>
        storedInvites.map(si =>
         AgentRequest(clientType, si.service, si.arn, agentRef.find(_.arn == si.arn).getOrElse(AgentReference.emptyAgentReference).uid, agencyName.getOrElse(si.arn, ""), si.status, si.expiryDate, si.lastUpdated, si.invitationId)
        ).sorted(AgentRequest.orderingByAgencyName).sorted(AgentRequest.orderingByLastUpdated)
    }
  }

  def invitations(identifierOpt: Option[TaxIdentifier])(f: TaxIdentifier => Future[Seq[StoredInvitation]]) = identifierOpt match {
    case Some(identifier) => f(identifier)
    case None => Future.successful(Seq.empty)
  }
}