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

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentclientmanagementfrontend.config.AppConfig
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpPost}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentServicesAccountConnector @Inject()(
                                               appConfig: AppConfig,
                                               http: HttpPost with HttpGet, metrics: Metrics) extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  val baseUrl = appConfig.agentServicesAccountBaseUrl

  implicit val mapReads: Reads[Map[Arn, String]] = new Reads[Map[Arn, String]] {
    override def reads(json: JsValue): JsResult[Map[Arn, String]] = JsSuccess {
      json.as[JsArray].value.map { details =>
        ((details \ "arn").as[Arn], (details \ "agencyName").as[String])
      }.toMap
    }
  }

  def getAgencyNames(arns: Seq[Arn])(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Map[Arn, String]] = {
    monitor(s"ConsumedAPI-AgencyNames-GET") {
      val url: String = s"$baseUrl/agent-services-account/client/agency-names"
      http.POST[Seq[String], JsValue](url, arns.map(_.value))
        .map { json => json.as[Map[Arn, String]] }
    }
  }
}


