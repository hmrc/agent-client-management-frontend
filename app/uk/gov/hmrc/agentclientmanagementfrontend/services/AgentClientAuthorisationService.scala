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
import play.api.libs.json.Reads
import uk.gov.hmrc.agentclientmanagementfrontend.connectors.{AgentClientAuthorisationConnector, AgentClientRelationshipsConnector, AgentServicesAccountConnector, PirRelationshipConnector}
import uk.gov.hmrc.agentclientmanagementfrontend.models._
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, InvitationId, MtdItId, Vrn}
import uk.gov.hmrc.domain.{Nino, TaxIdentifier}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class AgentClientAuthorisationService @Inject()(agentClientAuthorisationConnector: AgentClientAuthorisationConnector, agentClientRelationshipsConnector: AgentClientRelationshipsConnector, agentServicesAccountConnector: AgentServicesAccountConnector) {

  def getAgentRequests(mtdItId: MtdItId)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[AgentRequest]] = {
    val storedInvitations = agentClientAuthorisationConnector.getInvitation(mtdItId)
    val relationshipsWithAgencyNamesWithStoredInvitations = for {
      storedInvitations <- Future.sequence(Seq(storedInvitations)).map(_.flatten)
      agencyNames <- agentServicesAccountConnector.getAgencyNames(storedInvitations.map(_.arn))
    } yield (agencyNames, storedInvitations)

    relationshipsWithAgencyNamesWithStoredInvitations.map {
      case (agencyNames, storedInvites) =>
        storedInvites.map(si =>
          AgentRequest(si.service, agencyNames.getOrElse(si.arn, ""), si.status, si.expiryDate, si.lastUpdated.toLocalDate)
        )
    }
  }
}