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
import javax.inject.{Inject, Named, Singleton}

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import play.utils.UriEncoding
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentclientmanagementfrontend.util.Services
import uk.gov.hmrc.agentmtdidentifiers.model.MtdItId
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpReads}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DesConnector @Inject()(@Named("des-baseUrl") baseUrl: URL,
                             @Named("des.authorizationToken") authorizationToken: String,
                             @Named("des.environment") environment: String,
                             httpGet: HttpGet,
                             metrics: Metrics)
  extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  def getActiveClientItsaRelationships(mtdItId: MtdItId)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[String] = {
    val encodedClientId = UriEncoding.encodePathSegment(mtdItId.value, "UTF-8")
    val url = new URL(s"/registration/relationship?ref-no=$encodedClientId&agent=true&active-only=true&regime=${Services.ITSA}")

    getWithDesHeaders[String]("GetStatusAgentRelationship", url)
  }

  private def getWithDesHeaders[A: HttpReads](apiName: String, url: URL)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {
    val desHeaderCarrier = hc.copy(
      authorization = Some(Authorization(s"Bearer $authorizationToken")),
      extraHeaders = hc.extraHeaders :+ "Environment" -> environment)
    monitor(s"ConsumedAPI-DES-$apiName-GET") {
      httpGet.GET[A](url.toString)(implicitly[HttpReads[A]], desHeaderCarrier, ec)
    }
  }

}