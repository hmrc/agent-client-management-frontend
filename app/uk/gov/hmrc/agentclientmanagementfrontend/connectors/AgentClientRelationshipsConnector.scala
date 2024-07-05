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

package uk.gov.hmrc.agentclientmanagementfrontend.connectors

import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.agentclientmanagementfrontend.TaxIdentifierOps
import uk.gov.hmrc.agentclientmanagementfrontend.config.AppConfig
import uk.gov.hmrc.agentclientmanagementfrontend.models._
import uk.gov.hmrc.agentclientmanagementfrontend.util.HttpAPIMonitor
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentmtdidentifiers.model.Service.{HMRCCBCNONUKORG, HMRCCBCORG}
import uk.gov.hmrc.domain.TaxIdentifier
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AgentClientRelationshipsConnector @Inject() (appConfig: AppConfig, http: HttpClient, val metrics: Metrics)(implicit val ec: ExecutionContext)
    extends HttpAPIMonitor with Logging {

  val baseUrl = appConfig.agentClientRelationshipsBaseUrl

  def deleteRelationship(arn: Arn, clientId: TaxIdentifier, service: String)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    val deleteEndpoint =
      s"$baseUrl/agent-client-relationships/agent/${arn.value}/service/$service/client/${clientId.getIdTypeForAcr}/${clientId.value}"
    monitor(s"ConsumedAPI-AgentClientRelationship-${clientId.getGrafanaId}-DELETE") {
      http.DELETE[HttpResponse](deleteEndpoint).map(_.status == 204)
    }
  }

  def getActiveClientItsaRelationship(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[ItsaRelationship]] = {
    val url = s"$baseUrl/agent-client-relationships/client/relationships/service/HMRC-MTD-IT"
    monitor(s"ConsumedAPI-GetActiveRelationship-AgentClientRelationship-MTD-IT-GET") {
      http
        .GET[HttpResponse](url)
        .map(response =>
          response.status match {
            case OK =>
              val json = response.json
              (json \ "arn").asOpt[Arn].map(arn => ItsaRelationship(arn, (json \ "dateFrom").asOpt[LocalDate]))
            case NOT_FOUND =>
              None
            case s =>
              val message = s"Unexpected response: $s from: $url body: ${response.body}"
              logger.error(message)
              throw UpstreamErrorResponse(message, s)
          }
        )
    }
  }

  def getInactiveClientRelationships(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Seq[InactiveRelationship]] = {
    val url = s"$baseUrl/agent-client-relationships/client/relationships/inactive"
    monitor(s"ConsumedAPI-GetInactiveRelationships-AgentClientRelationship") {
      http
        .GET[HttpResponse](url)
        .map { response =>
          response.status match {
            case OK        => response.json.as[Seq[InactiveRelationship]]
            case NOT_FOUND => Seq.empty
            case s =>
              val message = s"Unexpected response: $s from: $url body: ${response.body}"
              logger.error(message)
              throw UpstreamErrorResponse(message, s)
          }
        }
    }
  }

  def getActiveClientVatRelationship(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[VatRelationship]] = {
    val url = s"$baseUrl/agent-client-relationships/client/relationships/service/HMRC-MTD-VAT"
    monitor(s"ConsumedAPI-GetActiveRelationship-AgentClientRelationship-MTD-VAT-GET") {
      http
        .GET[HttpResponse](url)
        .map { response =>
          response.status match {
            case OK =>
              val json = response.json
              (json \ "arn").asOpt[Arn].map(arn => VatRelationship(arn, (json \ "dateFrom").asOpt[LocalDate]))
            case NOT_FOUND =>
              None
            case s =>
              val message = s"Unexpected response: $s from: $url body: ${response.body}"
              logger.error(message)
              throw UpstreamErrorResponse(message, s)
          }
        }
    }
  }

  def getActiveClientTrustRelationship(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[TrustRelationship]] = {
    val url = s"$baseUrl/agent-client-relationships/client/relationships/service/HMRC-TERS-ORG"
    monitor(s"ConsumedAPI-GetActiveRelationship-AgentClientRelationship-HMRC-TERS-ORG-GET") {
      http
        .GET[HttpResponse](url)
        .map { response =>
          response.status match {
            case OK =>
              val json = response.json
              (json \ "arn").asOpt[Arn].map(arn => TrustRelationship(arn, (json \ "dateFrom").asOpt[LocalDate]))
            case NOT_FOUND =>
              None
            case s =>
              val message = s"Unexpected response: $s from: $url body: ${response.body}"
              logger.error(message)
              throw UpstreamErrorResponse(message, s)
          }
        }
    }
  }

  def getActiveClientTrustNtRelationship(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[TrustNtRelationship]] = {
    val url = s"$baseUrl/agent-client-relationships/client/relationships/service/HMRC-TERSNT-ORG"
    monitor(s"ConsumedAPI-GetActiveRelationship-AgentClientRelationship-HMRC-TERSNT-ORG-GET") {
      http
        .GET[HttpResponse](url)
        .map { response =>
          response.status match {
            case OK =>
              val json = response.json
              (json \ "arn").asOpt[Arn].map(arn => TrustNtRelationship(arn, (json \ "dateFrom").asOpt[LocalDate]))
            case NOT_FOUND =>
              None
            case s =>
              val message = s"Unexpected response: $s from: $url body: ${response.body}"
              logger.error(message)
              throw UpstreamErrorResponse(message, s)
          }
        }
    }
  }

  def getActiveClientCgtRelationship(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[CgtRelationship]] = {
    val url = s"$baseUrl/agent-client-relationships/client/relationships/service/HMRC-CGT-PD"
    monitor(s"ConsumedAPI-GetActiveRelationship-AgentClientRelationship-HMRC-CGT-PD-GET") {
      http
        .GET[HttpResponse](url)
        .map { response =>
          response.status match {
            case OK =>
              val json = response.json
              (json \ "arn").asOpt[Arn].map(arn => CgtRelationship(arn, (json \ "dateFrom").asOpt[LocalDate]))
            case NOT_FOUND =>
              None
            case s =>
              val message = s"Unexpected response: $s from: $url body: ${response.body}"
              logger.error(message)
              throw UpstreamErrorResponse(message, s)
          }
        }
    }
  }

  def getActiveClientPptRelationship(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[PptRelationship]] = {
    val url = s"$baseUrl/agent-client-relationships/client/relationships/service/HMRC-PPT-ORG"
    monitor(s"ConsumedAPI-GetActiveRelationship-AgentClientRelationship-HMRC-PPT-ORG-GET") {
      http
        .GET[HttpResponse](url)
        .map { response =>
          response.status match {
            case OK =>
              val json = response.json
              (json \ "arn").asOpt[Arn].map(arn => PptRelationship(arn, (json \ "dateFrom").asOpt[LocalDate]))
            case NOT_FOUND =>
              None
            case s =>
              val message = s"Unexpected response: $s from: $url body: ${response.body}"
              logger.error(message)
              throw UpstreamErrorResponse(message, s)
          }
        }
    }
  }

  def getActiveClientCbcRelationship(isUkUser: Boolean)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[Relationship]] = {

    val (enrolmentTag, cbcVariant) =
      if (isUkUser) (HMRCCBCORG, CbcUKRelationship.apply _) else (HMRCCBCNONUKORG, CbcNonUKRelationship.apply _)

    val url =
      s"$baseUrl/agent-client-relationships/client/relationships/service/$enrolmentTag"
    monitor(s"ConsumedAPI-GetActiveRelationship-AgentClientRelationship-$enrolmentTag-GET") {
      http
        .GET[HttpResponse](url)
        .map { response =>
          response.status match {
            case OK =>
              val json = response.json
              (json \ "arn").asOpt[Arn].map(arn => cbcVariant(arn, (json \ "dateFrom").asOpt[LocalDate]))
            case NOT_FOUND =>
              None
            case s =>
              val message = s"Unexpected response: $s from: $url body: ${response.body}"
              logger.error(message)
              throw UpstreamErrorResponse(message, s)
          }
        }
    }
  }

  def getActiveClientPlrRelationship(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[PlrRelationship]] = {
    val url = s"$baseUrl/agent-client-relationships/client/relationships/service/HMRC-PILLAR2-ORG"
    monitor(s"ConsumedAPI-GetActiveRelationship-AgentClientRelationship-HMRC-PILLAR2-ORG-GET") {
      http
        .GET[HttpResponse](url)
        .map { response =>
          response.status match {
            case OK =>
              val json = response.json
              (json \ "arn").asOpt[Arn].map(arn => PlrRelationship(arn, (json \ "dateFrom").asOpt[LocalDate]))
            case NOT_FOUND =>
              None
            case s =>
              val message = s"Unexpected response: $s from: $url body: ${response.body}"
              logger.error(message)
              throw UpstreamErrorResponse(message, s)
          }
        }
    }
  }

}
