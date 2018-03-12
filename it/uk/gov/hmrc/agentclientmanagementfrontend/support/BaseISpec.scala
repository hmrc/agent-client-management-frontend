package uk.gov.hmrc.agentclientmanagementfrontend.support

import com.google.inject.AbstractModule
import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentclientmanagementfrontend.services.SessionStoreService
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.agentclientmanagementfrontend.stubs.{AuthStubs, DataStreamStubs}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

class BaseISpec extends UnitSpec with GuiceOneServerPerSuite with WireMockSupport with AuthStubs with DataStreamStubs with MetricsTestSupport {

  override implicit lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder = {
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port" -> wireMockPort,
        "microservice.services.agent-fi-relationship.port" -> wireMockPort,
        "microservice.services.agent-fi-relationship.host" -> wireMockHost,
        "microservice.services.des.host" -> wireMockHost,
        "microservice.services.des.port" -> wireMockPort,
        "microservice.services.des.authorization-token" -> "someToken",
        "microservice.services.des.environment" -> "",
        "microservice.services.agent-services-account.host" -> wireMockHost,
        "microservice.services.agent-client-relationships.host" -> wireMockHost,
        "microservice.services.agent-client-relationships.port" -> wireMockPort,
        "microservice.services.agent-services-account.port" -> wireMockPort,
        "microservice.services.cachable.session-cache.host" -> wireMockHost,
        "microservice.services.cachable.session-cache.port" -> wireMockPort,
        "microservice.services.cachable.session-cache.domain" -> "someDomain",
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

  override def commonStubs(): Unit = {
    givenCleanMetricRegistry()
    givenAuditConnector()
  }

  protected implicit val materializer = app.materializer

  protected lazy val sessionStoreService = new TestSessionStoreService

  protected def checkHtmlResultWithBodyText(result: Result, expectedSubstring: String): Unit = {
    contentType(result) shouldBe Some("text/html")
    charset(result) shouldBe Some("utf-8")
    bodyOf(result) should include(expectedSubstring)
  }

  private val messagesApi = app.injector.instanceOf[MessagesApi]
  private implicit val messages: Messages = messagesApi.preferred(Seq.empty[Lang])

  protected def htmlEscapedMessage(key: String): String = HtmlFormat.escape(Messages(key)).toString

  implicit def hc(implicit request: FakeRequest[_]): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

}
