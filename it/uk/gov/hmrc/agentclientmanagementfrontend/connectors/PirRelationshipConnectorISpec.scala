package uk.gov.hmrc.agentclientmanagementfrontend.connectors

import uk.gov.hmrc.agentclientmanagementfrontend.models.PirRelationship
import uk.gov.hmrc.agentclientmanagementfrontend.stubs.PirRelationshipStub
import uk.gov.hmrc.agentclientmanagementfrontend.support.BaseISpec
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class PirRelationshipConnectorISpec extends BaseISpec with PirRelationshipStub {

  implicit val hc = HeaderCarrier()
  val connector = app.injector.instanceOf[PirRelationshipConnector]
  val arn = Arn("TARN0000001")
  val clientId = MtdItId("AA123456A")
  val afiService = "PERSONAL-INCOME-RECORD"


  "Get client relationships" should {
    "return existing ACTIVE relationships for specified clientId" in {
      getActiveAfiRelationship(arn, afiService, clientId.value, false)

      val result = await(connector.getClientRelationships(clientId))

      result.isDefined shouldBe true
      result.get.isInstanceOf[List[PirRelationship]] shouldBe true
      result.get.head.arn shouldBe arn
    }
    "return NotFound Exception when ACTIVE relationship not found" in {
      getNotFoundForAfiRelationship(afiService, clientId.value)

      val result = await(connector.getClientRelationships(clientId))

      result.isDefined shouldBe false
      result shouldBe None
    }
  }
}
