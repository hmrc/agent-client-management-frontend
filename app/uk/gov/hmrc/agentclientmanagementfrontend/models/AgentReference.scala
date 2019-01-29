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

package uk.gov.hmrc.agentclientmanagementfrontend.models

import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn

case class AgentReference(arn: Arn, uid: String) {


}

object AgentReference {
  implicit val format = Json.format[AgentReference]
  implicit val reads: Reads[NinoBusinessDetails] = Json.reads[NinoBusinessDetails]

  val emptyAgentReference = AgentReference(Arn(""), "")
}
