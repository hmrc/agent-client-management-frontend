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

import java.time.LocalDate
import play.api.libs.json.{Json, OFormat}

case class AuthorisedAgent(uuId: String, serviceName: String, agencyName: String, dateFrom: Option[LocalDate])

object AuthorisedAgent {
  implicit val format: OFormat[AuthorisedAgent] = Json.format[AuthorisedAgent]

  implicit def dateOrdering: Ordering[LocalDate] = Ordering.fromLessThan(_ isAfter _)

  val orderingByDateFrom: Ordering[AuthorisedAgent] = Ordering.by(_.dateFrom)

  val orderingByAgencyName: Ordering[AuthorisedAgent] = Ordering.by(_.agencyName.toLowerCase)
}
