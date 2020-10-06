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

package uk.gov.hmrc.agentclientmanagementfrontend.connectors

import java.net.URL
import java.time.{LocalDate, LocalDateTime}

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import javax.inject.Inject
import play.api.Logger
import play.api.http.Status.{NOT_FOUND, NO_CONTENT}
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsArray, JsObject, JsPath, JsResult, JsSuccess, JsValue, Json, Reads}
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentclientmanagementfrontend.TaxIdentifierOps
import uk.gov.hmrc.agentclientmanagementfrontend.config.AppConfig
import uk.gov.hmrc.agentclientmanagementfrontend.models.{AgentReference, StoredInvitation, SuspensionDetails, SuspensionDetailsNotFound}
import uk.gov.hmrc.agentmtdidentifiers.model._
import uk.gov.hmrc.domain.{SimpleObjectReads, TaxIdentifier}
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

class AgentClientAuthorisationConnector @Inject()(appConfig: AppConfig,
                                                  http: HttpClient, metrics: Metrics) extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry
  import StoredReads._

  private val baseUrl = appConfig.agentClientAuthorisationBaseUrl

  def getInvitation(clientId: TaxIdentifier)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[StoredInvitation]] = {
    val url = s"$baseUrl/agent-client-authorisation/clients/${clientId.getIdTypeForAca}/${clientId.value}/invitations/received"
    monitor(s"ConsumedAPI-Client-${clientId.getGrafanaId}-Invitations-GET") {
      http.GET[JsObject](url).map(obj => (obj \ "_embedded" \ "invitations").as[Seq[StoredInvitation]]).recover {
        case e: NotFoundException => Seq.empty
      }
    }
  }

  def getAgentReferences(arns: Seq[Arn])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[AgentReference]] = {
    Future.sequence(arns.map(arn => {
      val url = s"$baseUrl/agencies/references/arn/${arn.value}"
      monitor("ConsumedAPI-Agent-Reference-Invitations-GET") {
        http.GET[AgentReference](url.toString).map(Some(_)).recover {
          case e =>
            Logger.warn(s"error during getting agency reference for arn: $arn, error: ${e.getMessage}")
            None
        }
      }
    })).map(_.collect {
      case Some(agentRef) => agentRef
    })
  }

  implicit val mapReads: Reads[Map[Arn, String]] = new Reads[Map[Arn, String]] {
    override def reads(json: JsValue): JsResult[Map[Arn, String]] = JsSuccess {
      json.as[JsArray].value.map { details =>
        ((details \ "arn").as[Arn], (details \ "agencyName").as[String])
      }.toMap
    }
  }

  def getAgencyNames(arns: Seq[Arn])(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Map[Arn, String]] = {
    monitor(s"ConsumedAPI-AgencyNames-GET") {
      val url: String = s"$baseUrl/agent-client-authorisation/client/agency-names"
      http.POST[Seq[String], JsValue](url, arns.map(_.value))
        .map { json => json.as[Map[Arn, String]] }
    }
  }

  def getSuspensionDetails(arn: Arn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SuspensionDetails] =
    monitor("ConsumerAPI-Get-AgencySuspensionDetails-GET") {
      http
        .GET[HttpResponse](s"$baseUrl/agent-client-authorisation/client/suspension-details/${arn.value}")
        .map(response =>
          response.status match {
            case 200 => Json.parse(response.body).as[SuspensionDetails]
            case 204 => SuspensionDetails(suspensionStatus = false, None)
          })
    } recoverWith {
      case _: NotFoundException => Future failed SuspensionDetailsNotFound("No record found for this agent")
    }

  def setRelationshipEnded(invitationId: InvitationId)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Option[Boolean]] =
    monitor("ConsumedApi-Set-Relationship-Ended-PUT") {
      val url = new URL(s"$baseUrl/agent-client-authorisation/invitations/${invitationId.value}/relationship-ended?endedBy=Client")
      http.PUT[String, HttpResponse](url.toString, "").map { r =>
        r.status match {
          case NO_CONTENT => Some(true)
          case NOT_FOUND  => Some(false)
          case _          => None
        }
      }
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
