package uk.gov.hmrc.agentclientmanagementfrontend.connectors

import uk.gov.hmrc.agentclientmanagementfrontend.stubs.AgentServicesAccountStub
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentclientmanagementfrontend.support.BaseISpec
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class AgentServicesAccountConnectorISpec extends BaseISpec with AgentServicesAccountStub {

  implicit val hc = HeaderCarrier()
  val connector = app.injector.instanceOf[AgentServicesAccountConnector]
  val nino = Nino("AB123456C")

  "getAgencyNames" should {
    val arnSeq = Seq(Arn("TARN0000010"), Arn("TARN0000011"))
    "when supplied valid Seq[Arn] return JsValue with Map[Arn, String]" in {
      getTwoAgencyNamesMap200((Arn("TARN0000010"),"someName"),(Arn("TARN0000011"),"someName1"))

      val result = await(connector.getAgencyNames(arnSeq))
      result.getOrElse(Arn("TARN0000010"), "") shouldBe "someName"
      result.getOrElse(Arn("TARN0000011"), "") shouldBe "someName1"
      result.size shouldBe 2
    }

    "when supplied invalid Arn in sequence return BadRequest" in {
      getAgencyNamesMap400("someInvalidArn")

      an[Exception] should be thrownBy await(connector.getAgencyNames(Seq(Arn("a"))))
    }
  }

  "getNino" should {
    "return some nino when agent's mtdbsa identifier is known to ETMP" in {
      givenNinoIsKnownFor(nino)
      givenAuditConnector()
      await(connector.getNino) shouldBe nino
    }

    "return nothing when agent's mtdbsa identifier is unknown to ETMP" in {
      givenNinoIsUnknownFor
      givenAuditConnector()
      an[Exception] should be thrownBy await(connector.getNino)
    }

    "fail when the service is unavailable" in {
      givenGetNinoReturnsServiceUnavailable
      givenAuditConnector()
      an[Exception] should be thrownBy await(connector.getNino)
    }

    "fail when the service is throwing errors" in {
      givenGetNinoReturnsServerError
      givenAuditConnector()
      an[Exception] should be thrownBy await(connector.getNino)
    }
  }
}
