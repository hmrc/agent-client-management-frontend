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
import java.time.{LocalDate, LocalDateTime}

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import javax.inject.Inject
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsObject, JsPath, Reads}
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentclientmanagementfrontend.TaxIdentifierOps
import uk.gov.hmrc.agentclientmanagementfrontend.config.AppConfig
import uk.gov.hmrc.agentclientmanagementfrontend.models.{AgentReference, StoredInvitation}
import uk.gov.hmrc.agentmtdidentifiers.model._
import uk.gov.hmrc.domain.{SimpleObjectReads, TaxIdentifier}
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

class AgentClientAuthorisationConnector @Inject()(appConfig: AppConfig,
                                                  http: HttpDelete with HttpGet, metrics: Metrics) extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry
  import StoredReads._

  def getInvitation(clientId: TaxIdentifier)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[StoredInvitation]] = {
    val url = s"${appConfig.agentClientAuthorisationBaseUrl}/agent-client-authorisation/clients/${clientId.getIdTypeForAca}/${clientId.value}/invitations/received"
    monitor(s"ConsumedAPI-Client-${clientId.getGrafanaId}-Invitations-GET") {
      http.GET[JsObject](url.toString).map(obj => (obj \ "_embedded" \ "invitations").as[Seq[StoredInvitation]]).recover {
        case e: NotFoundException => Seq.empty
      }
    }
  }

  def getAgentReferences(arns: Seq[Arn])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[AgentReference]] = {
    Future.sequence(arns.map(arn => {
      val url = s"${appConfig.agentClientAuthorisationBaseUrl}/agencies/references/arn/${arn.value}"
      monitor("ConsumedAPI-Agent-Reference-Invitations-GET") {
        http.GET[AgentReference](url.toString).map(obj => obj).recover {
          case e => throw new Exception(s"Agent Reference Not Found: $e")
        }
      }
    }))
  }

  object StoredReads {

      implicit val reads: Reads[StoredInvitation] = {

        implicit val urlReads: SimpleObjectReads[URL] = new SimpleObjectReads[URL]("href", s => new URL(new URL(appConfig.agentClientAuthorisationBaseUrl), s))

        ((JsPath \ "arn").read[Arn] and
          (JsPath \ "clientType").readNullable[String] and
          (JsPath \ "service").read[String] and
          (JsPath \ "clientId").read[String] and
          (JsPath \ "clientIdType").read[String] and
          (JsPath \ "suppliedClientId").read[String] and
          (JsPath \ "suppliedClientIdType").read[String] and
          (JsPath \ "status").read[String] and
          (JsPath \ "created").read[LocalDateTime] and
          (JsPath \ "lastUpdated").read[LocalDateTime] and
          (JsPath \ "expiryDate").read[LocalDate] and
          (JsPath \ "invitationId").read[String] and
          (JsPath \ "_links" \ "self").read[URL]) (
          (a, b, c, d, e, f, g, h, i, j, k, l, m) => StoredInvitation.apply(a, b, c, d, e, f, g, h, i, j, k, l, m)
        )
      }
  }

}
