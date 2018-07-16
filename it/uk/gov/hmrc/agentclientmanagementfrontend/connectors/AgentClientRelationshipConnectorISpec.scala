package uk.gov.hmrc.agentclientmanagementfrontend.connectors

import uk.gov.hmrc.agentclientmanagementfrontend.stubs.AgentClientRelationshipsStub
import uk.gov.hmrc.agentclientmanagementfrontend.support.BaseISpec
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, Vrn}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, Upstream5xxResponse}

import scala.concurrent.ExecutionContext.Implicits.global

class AgentClientRelationshipConnectorISpec extends BaseISpec with AgentClientRelationshipsStub {

  implicit val hc = HeaderCarrier()
  val connector = app.injector.instanceOf[AgentClientRelationshipsConnector]
  val arn = Arn("TARN0000001")
  val clientId = MtdItId("AA123456A")
  val validVrn = Vrn("101747641")
  val serviceItsa = "HMRC-MTD-IT"
  val serviceVat = "HMRC-MTD-VAT"
  val startDate = "2007-07-07"

  "Delete agent client relationships" should {
    "return true when a relationship for ITSA service has been deleted successfully" in {
      deleteActiveITSARelationship(arn.value, clientId.value)

      val result = await(connector.deleteItsaRelationship(arn, clientId))
      result shouldBe true
    }

    "return true when a relationship for VAT service has been deleted successfully" in {
      deleteActiveVATRelationship(arn.value, validVrn.value)

      val result = await(connector.deleteVatRelationship(arn, validVrn))
      result shouldBe true
    }

    "return false when a relationship for ITSA service is not found while deleting" in {
      deleteActiveITSARelationship(arn.value, clientId.value, 404)

      val result = await(connector.deleteItsaRelationship(arn, clientId))
      result shouldBe false
    }

    "return false when a relationship for VAT service is not found while deleting" in {
      deleteActiveVATRelationship(arn.value, validVrn.value, 404)

      val result = await(connector.deleteVatRelationship(arn, validVrn))
      result shouldBe false
    }

    "return exception for ITSA service when delete relationship throws bad request" in {
      deleteActiveITSARelationship(arn.value, clientId.value, 400)

      an[BadRequestException] should be thrownBy (await(connector.deleteItsaRelationship(arn, clientId)))
    }

    "return exception for VAT service when delete relationship throws bad request" in {
      deleteActiveVATRelationship(arn.value, validVrn.value, 400)

      an[BadRequestException] should be thrownBy (await(connector.deleteVatRelationship(arn, validVrn)))
    }

    "return exception for ITSA service when service returns bad gateway" in {
      deleteActiveITSARelationship(arn.value, clientId.value, 502)

      an[Upstream5xxResponse] should be thrownBy (await(connector.deleteItsaRelationship(arn, clientId)))
    }

    "return exception for VAT service when service returns bad gateway" in {
      deleteActiveVATRelationship(arn.value, validVrn.value, 502)

      an[Upstream5xxResponse] should be thrownBy (await(connector.deleteVatRelationship(arn, validVrn)))
    }

  }

  "Get active itsa relationship" should {
    "return existing active relationships for specified clientId" in {
      getClientActiveAgentRelationships(serviceItsa, arn.value, startDate)

      val result = await(connector.getActiveClientItsaRelationship)
      result.get.arn shouldBe arn
    }

    "return notFound active relationships for specified clientId" in {
      getNotFoundClientActiveAgentRelationships(serviceItsa)

      val result =  await(connector.getActiveClientItsaRelationship)
      result shouldBe None
    }
  }

  "Get active vat relationship" should {
    "return existing active relationships for specified clientId for VAT service" in {
      getClientActiveAgentRelationships(serviceVat, arn.value, startDate)

      val result = await(connector.getActiveClientVatRelationship)
      result.get.arn shouldBe arn
    }
    "return notFound active relationships for specified clientId" in {
      getNotFoundClientActiveAgentRelationships(serviceVat)

      val result =  await(connector.getActiveClientVatRelationship)
      result shouldBe None
    }
  }
}
