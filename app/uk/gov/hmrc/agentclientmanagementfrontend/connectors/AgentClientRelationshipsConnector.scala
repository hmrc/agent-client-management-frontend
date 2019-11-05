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
import java.time.LocalDate
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentclientmanagementfrontend.models.{ItsaRelationship, TrustRelationship, VatRelationship}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr, Vrn}
import uk.gov.hmrc.domain.TaxIdentifier
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

class AgentClientRelationshipsConnector @Inject()(@Named("agent-client-relationships-baseUrl") baseUrl: URL,
                                                  http: HttpDelete with HttpGet, metrics: Metrics) extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  def deleteItsaRelationship(arn: Arn, clientId: TaxIdentifier)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    val deleteEndpoint = new URL(baseUrl, s"/agent-client-relationships/agent/${arn.value}/service/HMRC-MTD-IT/client/MTDITID/${clientId.value}")
    monitor(s"ConsumedAPI-AgentClientRelationship-MTD-IT-DELETE") {
      http.DELETE[HttpResponse](deleteEndpoint.toString).map(_.status == 204) recover { case _: NotFoundException => false }
    }
  }

  def deleteVatRelationship(arn: Arn, clientId: Vrn)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    val deleteEndpoint = new URL(baseUrl, s"/agent-client-relationships/agent/${arn.value}/service/HMRC-MTD-VAT/client/VRN/${clientId.value}")
    monitor(s"ConsumedAPI-AgentClientRelationship-MTD-VAT-DELETE") {
      http.DELETE[HttpResponse](deleteEndpoint.toString).map(_.status == 204) recover { case _: NotFoundException => false }
    }
  }

  def deleteTrustRelationship(arn: Arn, clientId: Utr)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    val deleteEndpoint = new URL(baseUrl, s"/agent-client-relationships/agent/${arn.value}/service/HMRC-TERS-ORG/client/SAUTR/${clientId.value}")
    monitor(s"ConsumedAPI-AgentClientRelationship-HMRC-TERS-ORG-DELETE") {
      http.DELETE[HttpResponse](deleteEndpoint.toString).map(_.status == 204) recover { case _: NotFoundException => false }
    }
  }

  def getActiveClientItsaRelationship(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[ItsaRelationship]] = {
    val url = new URL(baseUrl, s"/agent-client-relationships/client/relationships/service/HMRC-MTD-IT")
    monitor(s"ConsumedAPI-GetActiveRelationship-AgentClientRelationship-MTD-IT-GET") {
      http.GET[HttpResponse](url.toString).map { response =>
        val arnOpt =  response.status match {
          case 200 => (response.json \ "arn").asOpt[Arn]
        }
        val dateFromOpt = response.status match {
          case 200 => (response.json \ "dateFrom").asOpt[LocalDate]
        }
        arnOpt.map(arn => ItsaRelationship(arn, dateFromOpt))
      }.recover {
        case _ : NotFoundException => None
      }
    }
  }

  def getActiveClientVatRelationship(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[VatRelationship]] = {
    val url = new URL(baseUrl, s"/agent-client-relationships/client/relationships/service/HMRC-MTD-VAT")
    monitor(s"ConsumedAPI-GetActiveRelationship-AgentClientRelationship-MTD-VAT-GET") {
      http.GET[HttpResponse](url.toString).map { response =>
        val arnOpt =  response.status match {
          case 200 => (response.json \ "arn").asOpt[Arn]
        }
        val dateFromOpt = response.status match {
          case 200 => (response.json \ "dateFrom").asOpt[LocalDate]
        }
        arnOpt.map(arn => VatRelationship(arn, dateFromOpt))
      }.recover {
        case _ : NotFoundException => None
      }
    }
  }

  def getActiveClientTrustRelationship(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[TrustRelationship]] = {
    val url = new URL(baseUrl, s"/agent-client-relationships/client/relationships/service/HMRC-TERS-ORG")
    monitor(s"ConsumedAPI-GetActiveRelationship-AgentClientRelationship-HMRC-TERS-ORG-GET") {
      http.GET[HttpResponse](url.toString).map { response =>
        val arnOpt =  response.status match {
          case 200 => (response.json \ "arn").asOpt[Arn]
        }
        val dateFromOpt = response.status match {
          case 200 => (response.json \ "dateFrom").asOpt[LocalDate]
        }
        arnOpt.map(arn => TrustRelationship(arn, dateFromOpt))
      }.recover {
        case _ : NotFoundException => None
      }
    }
  }
}
