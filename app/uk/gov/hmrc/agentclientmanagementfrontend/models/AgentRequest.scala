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

package uk.gov.hmrc.agentclientmanagementfrontend.models

import org.joda.time.LocalDate

case class AgentRequest(serviceName: String, agencyName:String, status: String, expiryDate: LocalDate, lastUpdated: LocalDate, invitationId: String, sortDate: LocalDate){

  def effectiveStatus(implicit now: LocalDate): String =
    if (status == "Pending" && (now.isAfter(expiryDate) || now.isEqual(expiryDate))) "Expired"
    else status
}

object AgentRequest {

  implicit def dateOrdering: Ordering[LocalDate] = Ordering.fromLessThan(_ isAfter _)

  val orderingByAgencyName: Ordering[AgentRequest] = Ordering.by(_.agencyName.toLowerCase)

  val orderingBySortDate: Ordering[AgentRequest] = Ordering.by(_.sortDate)
}