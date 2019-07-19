package uk.gov.hmrc.agentclientmanagementfrontend.connectors

import uk.gov.hmrc.agentclientmanagementfrontend.stubs.AgentServicesAccountStub
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentclientmanagementfrontend.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class AgentServicesAccountConnectorISpec extends BaseISpec with AgentServicesAccountStub {

  implicit val hc = HeaderCarrier()
  val connector = app.injector.instanceOf[AgentServicesAccountConnector]

  "getAgencyNames" should {
    val arnSeq = Seq(Arn("TARN0000010"), Arn("TARN0000011"), Arn("TARN0000012"), Arn("TARN0000013"))

    "when supplied valid Seq[Arn] return JsValue with Map[Arn, String]" in {
      getFourAgencyNamesMap200((Arn("TARN0000010"),"someName"),(Arn("TARN0000011"),"someName1"), (Arn("TARN0000012"),"someName2"), (Arn("TARN0000013"),"someName3"))

      val result = await(connector.getAgencyNames(arnSeq))
      result.getOrElse(Arn("TARN0000010"), "") shouldBe "someName"
      result.getOrElse(Arn("TARN0000011"), "") shouldBe "someName1"
      result.getOrElse(Arn("TARN0000012"), "") shouldBe "someName2"
      result.getOrElse(Arn("TARN0000013"), "") shouldBe "someName3"
      result.size shouldBe 4
    }

    "when supplied invalid Arn in sequence return BadRequest" in {
      getAgencyNamesMap400("someInvalidArn")

      an[Exception] should be thrownBy await(connector.getAgencyNames(Seq(Arn("someInvalidArn"))))
    }
  }
}
