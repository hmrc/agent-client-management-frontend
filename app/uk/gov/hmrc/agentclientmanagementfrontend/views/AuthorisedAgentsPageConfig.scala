/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.LocalDate

import play.api.mvc.Request
import uk.gov.hmrc.agentclientmanagementfrontend.models.{AgentRequest, AuthorisedAgent}
import uk.gov.hmrc.agentclientmanagementfrontend.util.DisplayDateUtils

case class AuthorisedAgentsPageConfig(authorisedAgents: Seq[AuthorisedAgent], agentRequests:Seq[AgentRequest])(implicit request: Request[_], dateOrdering: Ordering[LocalDate]) {

  //non suspended and non terminated
  val validPendingRequests: Seq[AgentRequest] = agentRequests
    .filter(x => x.status == "Pending" && !x.isSuspended && x.uid != "")
    .sortBy(_.expiryDate).map(x => x.arn -> x)
    .toMap.values.toSet.toSeq

  //non suspended and non terminated
  val validNonPendingRequests: Seq[AgentRequest] = agentRequests.filter(x => x.status != "Pending" && !x.isSuspended && x.uid != "")

  val validPendingRequestsExist: Boolean = validPendingRequests.nonEmpty

  val validNonPendingRequestsExist: Boolean = validNonPendingRequests.nonEmpty

  val authorisedAgentsExist: Boolean = authorisedAgents.nonEmpty

  val validPendingCount: Int = validPendingRequests.length

  def displayDate(date: Option[LocalDate]): String = DisplayDateUtils.displayDateForLang(date)

}


