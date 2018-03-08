/*
 * Copyright 2018 HM Revenue & Customs
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

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}
import uk.gov.hmrc.http.{HeaderCarrier, HttpDelete, HttpResponse, NotFoundException}

import scala.concurrent.{ExecutionContext, Future}

class AgentClientRelationshipsConnector @Inject()(
                                                   @Named("agent-client-relationships-baseUrl") baseUrl: URL,
                                                   http: HttpDelete, metrics: Metrics) extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  def deleteRelationship(arn: Arn, clientId: MtdItId)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    val deleteEndpoint = new URL(baseUrl, s"/agent-client-relationships/agent/${arn.value}/service/HMRC-MTD-IT/client/MTDITID/${clientId.value}")
    monitor(s"ConsumedAPI-Delete-AgentClientRelationship-DELETE") {
      http.DELETE[HttpResponse](deleteEndpoint.toString).map(_.status == 204) recover { case _: NotFoundException => false }
    }
  }
}
