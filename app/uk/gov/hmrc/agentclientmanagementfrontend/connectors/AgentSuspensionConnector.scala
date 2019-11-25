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

package uk.gov.hmrc.agentclientmanagementfrontend.connectors

import java.net.URL

import javax.inject.{Inject, Named}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.agentclientmanagementfrontend.config.AppConfig
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, NotFoundException}

import scala.concurrent.{ExecutionContext, Future}

class AgentSuspensionConnector @Inject()(appConfig: AppConfig, http: HttpGet) {

  def getSuspendedServices(arn: Arn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SuspensionResponse] =
    if(appConfig.enableAgentSuspension) {
    http
      .GET[SuspensionResponse](
        s"${appConfig.agentSuspensionBaseUrl}/agent-suspension/status/arn/${arn.value}"
      )
  } recoverWith {
    case _: NotFoundException => Future successful SuspensionResponse(Set.empty)
  }
  else Future successful SuspensionResponse(Set())
}

case class SuspensionResponse(services: Set[String]) {

  def isSuspended(s: String): Boolean = services.contains(s)

}

object SuspensionResponse {
  implicit val formats: OFormat[SuspensionResponse] = Json.format
}
