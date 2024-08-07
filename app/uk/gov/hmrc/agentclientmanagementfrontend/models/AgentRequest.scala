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

package uk.gov.hmrc.agentclientmanagementfrontend.models

import java.time.{LocalDate, LocalDateTime}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn

case class AgentRequest(
  clientType: String,
  serviceName: String,
  arn: Arn,
  uid: String,
  agencyName: String,
  status: String,
  expiryDate: LocalDate,
  lastUpdated: LocalDateTime,
  invitationId: String,
  isSuspended: Boolean = false,
  isRelationshipEnded: Boolean = false,
  relationshipEndedBy: Option[String] = None
)

object AgentRequest {

  implicit def timeOrdering: Ordering[LocalDateTime] = Ordering.fromLessThan(_ isAfter _)

  val orderingByAgencyName: Ordering[AgentRequest] = Ordering.by(_.agencyName.toLowerCase)

  val orderingByLastUpdated: Ordering[AgentRequest] = Ordering.by(_.lastUpdated)

  def toAuthorisedAgent(ar: AgentRequest): AuthorisedAgent =
    AuthorisedAgent(uuId = ar.uid, serviceName = ar.serviceName, agencyName = ar.agencyName, dateFrom = Some(ar.lastUpdated.toLocalDate))
}
