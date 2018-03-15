package uk.gov.hmrc.agentclientmanagementfrontend.controllers

import play.api.mvc.{ Request, Result }
import play.api.test.FakeRequest
import uk.gov.hmrc.agentclientmanagementfrontend.support.BaseISpec
import uk.gov.hmrc.auth.core.{ AuthConnector, AuthorisationException, InsufficientEnrolments }
import uk.gov.hmrc.http.{ HeaderCarrier, SessionKeys }
import play.api.mvc.Results._

import scala.concurrent.Future

class AuthActionsISpec extends BaseISpec {

  object TestController extends AuthActions {

    override def authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]

    implicit val hc = HeaderCarrier()
    implicit val request = FakeRequest().withSession(SessionKeys.authToken -> "Bearer XYZ")
    import scala.concurrent.ExecutionContext.Implicits.global

    def withAuthorisedAsAgent[A]: Result = {
      await(super.withAuthorisedAsAgent { arn => Future.successful(Ok(arn.value)) })
    }

    def withAuthorisedAsClient[A]: Result = {
      await(super.withAuthorisedAsClient { (mtdItIdOpt, ninoOpt) =>
        Future.successful(Ok(s"mtdItId: ${mtdItIdOpt.map(_.value).getOrElse("")} nino: ${ninoOpt.map(_.nino).getOrElse("")}")) })
    }

  }

  "withAuthorisedAsAgent" should {

    "call body with arn when valid agent" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"authorisedEnrolments": [
           |  { "key":"HMRC-AS-AGENT", "identifiers": [
           |    { "key":"AgentReferenceNumber", "value": "fooArn" }
           |  ]}
           |]}""".stripMargin)
      val result = TestController.withAuthorisedAsAgent
      status(result) shouldBe 200
      bodyOf(result) shouldBe "fooArn"
    }

    "throw AutorisationException when user not logged in" in {
      givenUnauthorisedWith("MissingBearerToken")
      an[AuthorisationException] shouldBe thrownBy {
        TestController.withAuthorisedAsAgent
      }
    }

    "throw InsufficientEnrolments when agent not enrolled for service" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"authorisedEnrolments": [
           |  { "key":"HMRC-MTD-IT", "identifiers": [
           |    { "key":"MTDITID", "value": "fooMtdItId" }
           |  ]}
           |]}""".stripMargin)
      an[InsufficientEnrolments] shouldBe thrownBy {
        TestController.withAuthorisedAsAgent
      }
    }

    "throw InsufficientEnrolments when expected agent's identifier missing" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"authorisedEnrolments": [
           |  { "key":"HMRC-AS-AGENT", "identifiers": [
           |    { "key":"BAR", "value": "fooArn" }
           |  ]}
           |]}""".stripMargin)
      an[InsufficientEnrolments] shouldBe thrownBy {
        TestController.withAuthorisedAsAgent
      }
    }
  }

  "withAuthorisedAsClient" should {

    "call body with mtditid when valid mtd client" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"authorisedEnrolments": [
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
           |"authorisedEnrolments": [
           |  { "key":"HMRC-NI", "identifiers": [
           |    { "key":"NI", "value": "AE123456A" }
           |  ]}
           |]}""".stripMargin)

      val result = TestController.withAuthorisedAsClient
      status(result) shouldBe 200
      bodyOf(result) should include("AE123456A")
    }

    "call body with nino and mtdItId when valid client" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"authorisedEnrolments": [
           |{ "key":"HMRC-MTD-IT", "identifiers": [
           |    { "key":"MTDITID", "value": "fooMtdItId" }
           |  ]},
           |  { "key":"HMRC-NI", "identifiers": [
           |    { "key":"NI", "value": "AE123456A" }
           |  ]}
           |]}""".stripMargin)

      val result = TestController.withAuthorisedAsClient
      status(result) shouldBe 200
      bodyOf(result) should include("fooMtdItId")
      bodyOf(result) should include("AE123456A")
    }

    "throw InsufficientEnrolments when client not enrolled for service" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"authorisedEnrolments": [
           |  { "key":"HMRC-AS-AGENT", "identifiers": [
           |    { "key":"AgentReferenceNumber", "value": "fooArn" }
           |  ]}
           |]}""".stripMargin)
      an[InsufficientEnrolments] shouldBe thrownBy {
        TestController.withAuthorisedAsClient
      }
    }

    "throw InsufficientEnrolments when expected client's identifier missing" in {
      givenAuthorisedFor(
        "{}",
        s"""{
           |"authorisedEnrolments": [
           |  { "key":"HMRC-MTD-IT", "identifiers": [
           |    { "key":"BAR", "value": "fooMtdItId" }
           |  ]}
           |]}""".stripMargin)
      an[InsufficientEnrolments] shouldBe thrownBy {
        TestController.withAuthorisedAsClient
      }
    }
  }

}
