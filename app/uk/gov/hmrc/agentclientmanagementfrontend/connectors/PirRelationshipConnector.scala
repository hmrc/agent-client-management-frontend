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
import javax.inject.{Inject, Named, Singleton}

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentclientmanagementfrontend.models.PirRelationship
import uk.gov.hmrc.agentclientmanagementfrontend.util.Services
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PirRelationshipConnector @Inject()(
                                          @Named("agent-fi-relationship-baseUrl") baseUrl: URL,
                                          http: HttpGet with HttpDelete,
                                          metrics: Metrics) extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  def getClientRelationships(nino: Nino)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Seq[PirRelationship]] = {
    monitor(s"ConsumedAPI-Get-AfiRelationships-GET") {
      val url = craftUrl(s"/agent-fi-relationship/relationships/service/${Services.HMRCPIR}/clientId/${nino.value}")
      http.GET[Seq[PirRelationship]](url.toString).recover {
        case e: NotFoundException => Seq.empty
      }
    }
  }

  def deleteClientRelationship(arn: Arn, nino: Nino)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    monitor(s"ConsumedAPI-Delete-AfiRelationship-DELETE") {
      val url = craftUrl(s"/agent-fi-relationship/relationships/agent/${arn.value}/service/${Services.HMRCPIR}/client/${nino.value}")
      http.DELETE[HttpResponse](url.toString).map(_.status == 200) recover { case _: NotFoundException => false }
    }
  }

  private def craftUrl(location: String) = new URL(baseUrl, location)
}
