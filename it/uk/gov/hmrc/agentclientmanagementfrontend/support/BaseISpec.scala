package uk.gov.hmrc.agentclientmanagementfrontend.support

import akka.stream.Materializer
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
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentclientmanagementfrontend.services.SessionStoreService
import uk.gov.hmrc.agentclientmanagementfrontend.stubs.AuthStubs
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.test.UnitSpec

import scala.collection.immutable

class BaseISpec extends UnitSpec with GuiceOneServerPerSuite with WireMockSupport with AuthStubs with MetricsTestSupport {

  def featureRemoveAuthorisationPir = false

  def featureRemoveAuthorisationITSA = false

  def featureRemoveAuthorisationVat = false

  def featureRemoveAuthorisationTrust = false

  override implicit lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder = {
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port" -> wireMockPort,
        "microservice.services.agent-fi-relationship.port" -> wireMockPort,
        "microservice.services.agent-fi-relationship.host" -> wireMockHost,
        "microservice.services.agent-client-relationships.host" -> wireMockHost,
        "microservice.services.agent-client-relationships.port" -> wireMockPort,
        "microservice.services.agent-client-authorisation.host" -> wireMockHost,
        "microservice.services.agent-client-authorisation.port" -> wireMockPort,
        "microservice.services.agent-suspension.host" -> wireMockHost,
        "microservice.services.agent-suspension.port" -> wireMockPort,
        "microservice.services.cachable.session-cache.host" -> wireMockHost,
        "microservice.services.cachable.session-cache.port" -> wireMockPort,
        "microservice.services.cachable.session-cache.domain" -> "someDomain",
        "features.remove-authorisation.PERSONAL-INCOME-RECORD" -> featureRemoveAuthorisationPir,
        "features.remove-authorisation.HMRC-MTD-IT" -> featureRemoveAuthorisationITSA,
        "features.remove-authorisation.HMRC-MTD-VAT" -> featureRemoveAuthorisationVat,
        "features.remove-authorisation.HMRC-TERS-ORG" -> featureRemoveAuthorisationTrust,
        "features.enable-agent-suspension" -> true,
        "metrics.enabled" -> true,
        "auditing.enabled" -> true,
        "auditing.consumer.baseUri.host" -> wireMockHost,
        "auditing.consumer.baseUri.port" -> wireMockPort).overrides(new TestGuiceModule)
  }

  private class TestGuiceModule extends AbstractModule {
    override def configure(): Unit = {
      bind(classOf[SessionStoreService]).toInstance(sessionStoreService)
    }
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    sessionStoreService.clear()
  }

  override def commonStubs(): immutable.Seq[StubMapping] = {
    givenCleanMetricRegistry()
    List(stubFor(
      post(urlEqualTo(s"/write/audit/merged"))
        .willReturn(aResponse().withStatus(204))
    ),
    stubFor(
      post(urlEqualTo(s"/write/audit"))
        .willReturn(aResponse().withStatus(204))
    ))
  }

  protected implicit val materializer: Materializer = app.materializer

  protected lazy val sessionStoreService = new TestSessionStoreService

  protected def checkHtmlResultWithBodyText(result: Result, expectedSubstring: String): Assertion = {
    contentType(result) shouldBe Some("text/html")
    charset(result) shouldBe Some("utf-8")
    bodyOf(result) should include(expectedSubstring)
  }

  protected def checkResponseBodyWithText(response: WSResponse, expectedText: String*): Unit = {
    for(text <- expectedText) {
      response.body.contains(text) shouldBe true
    }
  }

  protected def checkResponseBodyNotWithText(response: WSResponse, expectedText: String*): Unit = {
    for(text <- expectedText) {
      response.body.contains(text) shouldBe false
    }
  }

  private val messagesApi = app.injector.instanceOf[MessagesApi]
  private implicit val messages: Messages = messagesApi.preferred(Seq.empty[Lang])

  protected def htmlEscapedMessage(key: String): String = HtmlFormat.escape(Messages(key)).toString

  implicit def hc(implicit request: FakeRequest[_]): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

}
