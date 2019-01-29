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

package uk.gov.hmrc.agentclientmanagementfrontend.views

import org.joda.time.LocalDate
import play.api.i18n.Messages
import uk.gov.hmrc.agentclientmanagementfrontend.models.{AgentRequest, AuthorisedAgent}

case class AuthorisedAgentsPageConfig(authorisedAgents: Seq[AuthorisedAgent], agentRequests:Seq[AgentRequest])(implicit messages: Messages, dateOrdering: Ordering[LocalDate]) {

  val pendingRequests: Seq[AgentRequest] = agentRequests.filter(_.status == "Pending").sortBy(_.expiryDate).map(x => x.arn -> x).toMap.values.toSet.toSeq

  val nonPendingRequests: Seq[AgentRequest] = agentRequests.filter(_.status != "Pending")

  val pendingRequestsExist: Boolean = pendingRequests.nonEmpty

  val nonPendingRequestsExist: Boolean = nonPendingRequests.nonEmpty

  val authorisedAgentsExist: Boolean = authorisedAgents.nonEmpty

  def displayDate(date: Option[LocalDate]): String = {
    date match {
      case Some(d) if d.getDayOfMonth >= 10 => d.toString("dd MMMM yyyy", messages.lang.locale)
      case Some(d) if d.getDayOfMonth < 10 => d.toString("d MMMM yyyy", messages.lang.locale)
      case None => ""
    }
  }

  val pendingNo: String = {
    val count = pendingRequests.length
    if(count == 1) {
      Messages("client-authorised-agents-table-relationships.pendingNo.single", count)
    }else {
      Messages("client-authorised-agents-table-relationships.pendingNo", count)
    }
  }

}