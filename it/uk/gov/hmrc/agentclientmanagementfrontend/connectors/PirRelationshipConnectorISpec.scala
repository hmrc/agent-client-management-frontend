package uk.gov.hmrc.agentclientmanagementfrontend.connectors

import uk.gov.hmrc.agentclientmanagementfrontend.models.PirRelationship
import uk.gov.hmrc.agentclientmanagementfrontend.stubs.PirRelationshipStub
import uk.gov.hmrc.agentclientmanagementfrontend.support.BaseISpec
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global

class PirRelationshipConnectorISpec extends BaseISpec with PirRelationshipStub {

  implicit val hc = HeaderCarrier()
  val connector = app.injector.instanceOf[PirRelationshipConnector]
  val arn = Arn("TARN0000001")
  val clientId = "AA123456A"
  val afiService = "PERSONAL-INCOME-RECORD"


  "Get client relationships" should {
    "return existing ACTIVE relationships for specified clientId" in {
      getActiveAfiRelationship(arn, afiService, clientId, false)

      val result = await(connector.getClientRelationships(clientId))

      result.isDefined shouldBe true
      result.get.isInstanceOf[List[PirRelationship]] shouldBe true
      result.get.head.clientId shouldBe clientId
    }
    "return NotFound Exception when ACTIVE relationship not found" in {
      getNotFoundForAfiRelationship(afiService, clientId)

      val result = await(connector.getClientRelationships(clientId))

      result.isDefined shouldBe false
      result shouldBe None
    }
  }
}
