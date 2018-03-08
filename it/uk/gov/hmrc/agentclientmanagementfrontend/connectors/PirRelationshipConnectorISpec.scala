package uk.gov.hmrc.agentclientmanagementfrontend.connectors

import uk.gov.hmrc.agentclientmanagementfrontend.models.PirRelationship
import uk.gov.hmrc.agentclientmanagementfrontend.stubs.PirRelationshipStub
import uk.gov.hmrc.agentclientmanagementfrontend.support.BaseISpec
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, Upstream5xxResponse}

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


      result.isInstanceOf[Seq[PirRelationship]] shouldBe true
      result.head.arn shouldBe arn
    }
    "return NotFound Exception when ACTIVE relationship not found" in {
      getNotFoundForAfiRelationship(afiService, clientId.value)

      val result = await(connector.getClientRelationships(clientId))

      result shouldBe empty
    }
  }

  "Delete agent client relationships" should {
    "return true when a relationship has been deleted successfully" in {
      deleteActivePIRRelationship(arn.value, clientId.value)

      val result = await(connector.deleteClientRelationship(arn, clientId))
      result shouldBe true
    }

    "return false when a relationship is not found while deleting" in {
      deleteActivePIRRelationship(arn.value, clientId.value, 404)

      val result = await(connector.deleteClientRelationship(arn, clientId))
      result shouldBe false
    }

    "return exception when delete relationship throws bad request" in {
      deleteActivePIRRelationship(arn.value, clientId.value, 400)

      an[BadRequestException] should be thrownBy(await(connector.deleteClientRelationship(arn, clientId)))
    }

    "return exception when service returns bad gateway" in {
      deleteActivePIRRelationship(arn.value, clientId.value, 502)

      an[Upstream5xxResponse] should be thrownBy(await(connector.deleteClientRelationship(arn, clientId)))
    }
  }
}
