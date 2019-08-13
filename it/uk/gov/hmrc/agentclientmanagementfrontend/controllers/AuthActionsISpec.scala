package uk.gov.hmrc.agentclientmanagementfrontend.controllers

import play.api.mvc.Result
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

    def withAuthorisedAsClient[A]: Result = {
      await(super.withAuthorisedAsClient { (clientType, clientIds) =>
        Future.successful(Ok(s"clientType: $clientType, mtdItId: ${clientIds.mtdItId.map(_.value).getOrElse("")} nino: ${clientIds.nino.map(_.nino).getOrElse("")} vrn: ${clientIds.vrn.map(_.value).getOrElse("")} utr: ${clientIds.utr.map(_.value).getOrElse("")}")) })
    }

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

    "call body with nino, mtdItId, vrn and utr when valid client" in {
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
           |  ]}
           |]}""".stripMargin)

      val result = TestController.withAuthorisedAsClient
      status(result) shouldBe 200
      bodyOf(result) should include("fooMtdItId")
      bodyOf(result) should include("AE123456A")
      bodyOf(result) should include("fooVrn")
      bodyOf(result) should include("fooUtr")
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
        TestController.withAuthorisedAsClient shouldBe Forbidden

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
      TestController.withAuthorisedAsClient shouldBe Forbidden
    }
  }

}
