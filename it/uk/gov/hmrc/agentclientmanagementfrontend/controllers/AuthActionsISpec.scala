package uk.gov.hmrc.agentclientmanagementfrontend.controllers

import play.api.http.Status.SEE_OTHER
import play.api.i18n.Messages
import play.api.mvc.Results._
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.{Configuration, Environment}
import play.twirl.api.Html
import uk.gov.hmrc.agentclientmanagementfrontend.support.BaseISpec
import uk.gov.hmrc.agentclientmanagementfrontend.views.html.error_template
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthActionsISpec extends BaseISpec {

  object TestController extends AuthActions {

    override def authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]

    override def env: Environment = app.injector.instanceOf[Environment]

    override def config: Configuration = app.configuration

    implicit val hc = HeaderCarrier()
    implicit val request = FakeRequest().withSession(SessionKeys.authToken -> "Bearer XYZ")

    val errorTemplate = app.injector.instanceOf[error_template]

    def withAuthorisedAsClient[A]: Result = {
      await(super.withAuthorisedAsClient { (clientType, clientIds, _) =>
        Future.successful(Ok(s"clientType: $clientType, mtdItId: ${clientIds.mtdItId.map(_.value).getOrElse("")} nino: ${clientIds.nino.map(_.nino).getOrElse("")} vrn: ${clientIds.vrn.map(_.value).getOrElse("")} urn: ${clientIds.urn.map(_.value).getOrElse("")} utr: ${clientIds.utr.map(_.value).getOrElse("")}")) })
    }

    override def forbiddenView(implicit request: Request[_]): Html = errorTemplate(
      Messages("global.error.403.title"),
      Messages("global.error.403.heading"),
      Messages("global.error.403.message")
    )
  }

  "withAuthorisedAsClient" should {

    "call body with mtditid when valid mtd client" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"affinityGroup":"Individual",
           |"allEnrolments": [
           |  { "key":"HMRC-MTD-IT", "identifiers": [
           |    { "key":"MTDITID", "value": "fooMtdItId" }
           |  ]}
           |]}""".stripMargin)

      val result = TestController.withAuthorisedAsClient
      status(result) shouldBe 200
      bodyOf(result) should include("fooMtdItId")
    }

    "call body with nino when valid nino client" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"affinityGroup":"Individual",
           |"allEnrolments": [
           |  { "key":"HMRC-NI", "identifiers": [
           |    { "key":"NINO", "value": "AE123456A" }
           |  ]}
           |]}""".stripMargin)

      val result = TestController.withAuthorisedAsClient
      status(result) shouldBe 200
      bodyOf(result) should include("AE123456A")
    }

    "call body with urn when valid urn client" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"affinityGroup":"Individual",
           |"allEnrolments": [
           |  { "key":"HMRC-TERSNT-ORG", "identifiers": [
           |    { "key":"URN", "value": "AE12345" }
           |  ]}
           |]}""".stripMargin)

      val result = TestController.withAuthorisedAsClient
      status(result) shouldBe 200
      bodyOf(result) should include("AE12345")
    }

    "call body with vrn when valid VAT client" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"affinityGroup":"Individual",
           |"allEnrolments": [
           |  { "key":"HMRC-MTD-VAT", "identifiers": [
           |    { "key":"VRN", "value": "fooVrn" }
           |  ]}
           |]}""".stripMargin)

      val result = TestController.withAuthorisedAsClient
      status(result) shouldBe 200
      bodyOf(result) should include("fooVrn")
    }

    "call body with nino, mtdItId, vrn, urn and utr when valid client" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"affinityGroup":"Individual",
           |"allEnrolments": [
           |{ "key":"HMRC-MTD-IT", "identifiers": [
           |    { "key":"MTDITID", "value": "fooMtdItId" }
           |  ]},
           |  { "key":"HMRC-NI", "identifiers": [
           |    { "key":"NINO", "value": "AE123456A" }
           |  ]},
           |  { "key":"HMRC-MTD-VAT", "identifiers": [
           |    { "key":"VRN", "value": "fooVrn" }
           |  ]},
           |  { "key":"HMRC-TERS-ORG", "identifiers": [
           |    { "key":"SAUTR", "value": "fooUtr" }
           |  ]},
           |  { "key":"HMRC-TERSNT-ORG", "identifiers": [
           |    { "key":"URN", "value": "AE12345NT" }
           |  ]}
           |]}""".stripMargin)

      val result = TestController.withAuthorisedAsClient
      status(result) shouldBe 200
      bodyOf(result) should include("fooMtdItId")
      bodyOf(result) should include("AE123456A")
      bodyOf(result) should include("fooVrn")
      bodyOf(result) should include("fooUtr")
      bodyOf(result) should include("AE12345NT")
    }

    "throw Forbidden when client not enrolled for service" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"affinityGroup":"Individual",
           |"allEnrolments": [
           |  { "key":"HMRC-AS-AGENT", "identifiers": [
           |    { "key":"AgentReferenceNumber", "value": "fooArn" }
           |  ]}
           |]}""".stripMargin)

      val result = TestController.withAuthorisedAsClient
      status(result) shouldBe 403
      bodyOf(result) should include("Sorry, you haven’t been authorised to proceed")

    }

    "throw Forbidden when expected client's identifier missing" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"affinityGroup":"Individual",
           |"allEnrolments": [
           |  { "key":"HMRC-MTD-IT", "identifiers": [
           |    { "key":"BAR", "value": "fooMtdItId" }
           |  ]}
           |]}""".stripMargin)

      val result = TestController.withAuthorisedAsClient
      status(result) shouldBe 403
      bodyOf(result) should include("Sorry, you haven’t been authorised to proceed")
    }

    "redirect to GG login page if user is not logged in" in {
      givenUnauthorisedWith("BearerTokenExpired")
      TestController.withAuthorisedAsClient shouldBe Redirect("http://localhost:9553/bas-gateway/sign-in?continue_url=http://localhost:9568/&origin=agent-client-management-frontend", SEE_OTHER)
    }
  }

}
