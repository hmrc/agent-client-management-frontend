/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentclientmanagementfrontend.support

import org.apache.pekko.stream.Materializer
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import com.google.inject.AbstractModule
import org.scalatest.Assertion
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSResponse
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentclientmanagementfrontend.config.AppConfig
import uk.gov.hmrc.agentclientmanagementfrontend.services.MongoDBSessionStoreService
import uk.gov.hmrc.agentclientmanagementfrontend.stubs.AuthStubs
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.collection.immutable

class BaseISpec extends UnitSpec with GuiceOneServerPerSuite with WireMockSupport with AuthStubs with MetricsTestSupport {

  override implicit lazy val app: Application = appBuilder.build()

  protected def additionalConfiguration: Map[String, Any] =
    Map(
      "microservice.services.auth.port"                       -> wireMockPort,
      "microservice.services.agent-fi-relationship.port"      -> wireMockPort,
      "microservice.services.agent-fi-relationship.host"      -> wireMockHost,
      "microservice.services.agent-client-relationships.host" -> wireMockHost,
      "microservice.services.agent-client-relationships.port" -> wireMockPort,
      "microservice.services.agent-client-authorisation.host" -> wireMockHost,
      "microservice.services.agent-client-authorisation.port" -> wireMockPort,
      "metrics.enabled"                                       -> true,
      "auditing.enabled"                                      -> true,
      "bas-gateway.url"                                       -> s"http://localhost:$wireMockPort/bas-gateway/sign-in"
    )

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(additionalConfiguration)
      .overrides(new TestGuiceModule)

  private class TestGuiceModule extends AbstractModule {
    override def configure(): Unit =
      bind(classOf[MongoDBSessionStoreService]).toInstance(sessionStoreService)
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    sessionStoreService.clear()
  }

  override def commonStubs(): immutable.Seq[StubMapping] = {
    givenCleanMetricRegistry()
    List(
      stubFor(
        post(urlEqualTo(s"/write/audit/merged"))
          .willReturn(aResponse().withStatus(204))
      ),
      stubFor(
        post(urlEqualTo(s"/write/audit"))
          .willReturn(aResponse().withStatus(204))
      )
    )
  }

  protected implicit val materializer: Materializer = app.materializer

  protected lazy val sessionStoreService = new TestSessionStoreService

  protected def checkHtmlResultWithBodyText(result: Result, expectedSubstring: String): Assertion = {
    contentType(result) shouldBe Some("text/html")
    charset(result) shouldBe Some("utf-8")
    bodyOf(result) should include(expectedSubstring)
  }

  protected def checkHtmlResultWithBodyText(result: Result, expectedSubstrings: String*): Unit = {
    contentType(result) shouldBe Some("text/html")
    charset(result) shouldBe Some("utf-8")
    expectedSubstrings.foreach(s => bodyOf(result) should include(s))
  }

  protected def checkHtmlResultNotWithBodyText(result: Result, expectedSubstrings: String*): Unit = {
    contentType(result) shouldBe Some("text/html")
    charset(result) shouldBe Some("utf-8")
    expectedSubstrings.foreach(s => bodyOf(result) should not include s)
  }

  protected def checkResponseBodyWithText(response: WSResponse, expectedText: String*): Unit =
    for (text <- expectedText)
      response.body.contains(text) shouldBe true

  protected def checkResponseBodyNotWithText(response: WSResponse, expectedText: String*): Unit =
    for (text <- expectedText)
      response.body.contains(text) shouldBe false

  private val messagesApi = app.injector.instanceOf[MessagesApi]
  implicit val messages: Messages = messagesApi.preferred(Seq.empty[Lang])

  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  protected def htmlEscapedMessage(key: String): String = HtmlFormat.escape(Messages(key)).toString

  implicit def hc(implicit request: FakeRequest[_]): HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

}
