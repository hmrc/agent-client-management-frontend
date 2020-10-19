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

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentclientmanagementfrontend.config.AppConfig
import uk.gov.hmrc.agentclientmanagementfrontend.models.{PirInactiveRelationship, PirRelationship}
import uk.gov.hmrc.agentclientmanagementfrontend.util.Services
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PirRelationshipConnector @Inject()(
                                          appConfig: AppConfig,
                                          http: HttpClient,
                                          metrics: Metrics) extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  val baseUrl: String = appConfig.agentFiRelationshipBaseUrl

  def getClientRelationships(nino: Nino)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Seq[PirRelationship]] = {
    monitor(s"ConsumedAPI-AfiRelationships-GET") {
      val url = s"$baseUrl/agent-fi-relationship/relationships/service/${Services.HMRCPIR}/clientId/${nino.value}"
      http.GET[Seq[PirRelationship]](url.toString).recover {
        case e: NotFoundException => Seq.empty
      }
    }
  }

  def getInactiveClientRelationships()(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Seq[PirInactiveRelationship]] = {
    monitor(s"ConsumedAPI-AfiRelationships-GET") {
      val url = s"$baseUrl/agent-fi-relationship/relationships/inactive"
      http.GET[Seq[PirInactiveRelationship]](url).recover {
        case _: NotFoundException => Seq.empty
      }
    }
  }

  def deleteClientRelationship(arn: Arn, nino: Nino)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    monitor(s"ConsumedAPI-AfiRelationship-DELETE") {
      val url = s"$baseUrl/agent-fi-relationship/relationships/agent/${arn.value}/service/${Services.HMRCPIR}/client/${nino.value}"
      http.DELETE[HttpResponse](url.toString).map(_.status == 200) recover { case _: NotFoundException => false }
    }
  }
}
