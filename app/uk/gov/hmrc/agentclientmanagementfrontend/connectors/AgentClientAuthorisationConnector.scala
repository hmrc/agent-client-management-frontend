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

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Named}

import org.joda.time.{DateTime, LocalDate}
import play.api.libs.json.{JsObject, Json, Reads}
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentclientmanagementfrontend.models.{AgentReference, AuthorisedAgent, StoredInvitation}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, InvitationId, MtdItId, Vrn}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

class AgentClientAuthorisationConnector @Inject()(@Named("agent-client-authorisation-baseUrl") baseUrl: URL,
                                                  http: HttpDelete with HttpGet, metrics: Metrics) extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  import StoredReads._


  def getItsaInvitation(mtdItId: MtdItId)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[StoredInvitation]] = {
    val url = new URL(baseUrl, s"/agent-client-authorisation/clients/MTDITID/${mtdItId.value}/invitations/received")
    http.GET[JsObject](url.toString).map(obj => (obj \ "_embedded" \ "invitations").as[Seq[StoredInvitation]]).recover{
      case e: NotFoundException => Seq.empty
    }
  }

  def getVatInvitation(vrn: Vrn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[StoredInvitation]] = {
    val url = new URL(baseUrl, s"/agent-client-authorisation/clients/VRN/${vrn.value}/invitations/received")
    http.GET[JsObject](url.toString).map(obj => (obj \ "_embedded" \ "invitations").as[Seq[StoredInvitation]]).recover{
      case e: NotFoundException => Seq.empty
    }
  }

  def getIrvInvitation(nino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[StoredInvitation]] = {
    val url = new URL(baseUrl, s"/agent-client-authorisation/clients/NI/${nino.value}/invitations/received")
    http.GET[JsObject](url.toString).map(obj => (obj \ "_embedded" \ "invitations").as[Seq[StoredInvitation]]).recover{
      case e: NotFoundException => Seq.empty
    }
  }

  def getAgentReferences(arns: Seq[Arn])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[AgentReference]] = {
    Future.sequence(arns.map (arn => {
      val url = new URL(baseUrl, s"/agencies/references/arn/${arn.value}")
      http.GET[AgentReference](url.toString).map(obj => obj).recover {
        case e => throw new Exception("Agent Reference Not Found")
      }
    }))
  }

  object StoredReads {
      import play.api.libs.functional.syntax._
      import play.api.libs.json.{JsPath, Reads}
      import uk.gov.hmrc.domain.SimpleObjectReads
      import uk.gov.hmrc.http.controllers.RestFormats.dateTimeFormats

      implicit val reads: Reads[StoredInvitation] = {

        implicit val urlReads: SimpleObjectReads[URL] = new SimpleObjectReads[URL]("href", s => new URL(baseUrl, s))

        ((JsPath \ "arn").read[Arn] and
          (JsPath \ "clientType").read[String] and
          (JsPath \ "service").read[String] and
          (JsPath \ "clientId").read[String] and
          (JsPath \ "clientIdType").read[String] and
          (JsPath \ "suppliedClientId").read[String] and
          (JsPath \ "suppliedClientIdType").read[String] and
          (JsPath \ "status").read[String] and
          (JsPath \ "created").read[DateTime] and
          (JsPath \ "lastUpdated").read[DateTime] and
          (JsPath \ "expiryDate").read[LocalDate] and
          (JsPath \ "invitationId").read[String] and
          (JsPath \ "_links" \ "self").read[URL]) (
          (a, b, c, d, e, f, g, h, i, j, k, l, m) => StoredInvitation.apply(a, b, c, d, e, f, g, h, i, j, k, l, m)
        )
      }
  }

}
