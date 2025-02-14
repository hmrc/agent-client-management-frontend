/*
 * Copyright 2025 HM Revenue & Customs
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

package filters

import org.apache.pekko.stream.Materializer
import play.api.http.Status.MOVED_PERMANENTLY
import play.api.mvc._
import uk.gov.hmrc.agentclientmanagementfrontend.config.AppConfig

import java.net.URI
import javax.inject._
import scala.concurrent.Future

@Singleton
class RedirectACRFFilter @Inject() (implicit val mat: Materializer, val appConfig: AppConfig) extends Filter {

  private val baseUrl = appConfig.agentClientRelationshipsFrontendBaseUrl
  private val isRedirectToACRFEnabled: Boolean = appConfig.redirectToACRF

  override def apply(nextFilter: RequestHeader => Future[Result])(request: RequestHeader): Future[Result] =
    if (isRedirectToACRFEnabled) {

      // ACRF default URL with params
      val urlPatternRootWithParams = s"""^$baseUrl(\\?.*)?$$""".r

      val redirectUrlWithParams = s"$baseUrl${Option(new URI(request.uri).getQuery).map("?" + _).getOrElse("")}"
      val redirectUrl =
        if (urlPatternRootWithParams.matches(redirectUrlWithParams))
          redirectUrlWithParams
        else baseUrl

      Future.successful(Results.Redirect(redirectUrl, MOVED_PERMANENTLY))

    } else {
      nextFilter(request)
    }

}
