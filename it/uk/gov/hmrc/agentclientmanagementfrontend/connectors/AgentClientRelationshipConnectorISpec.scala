package uk.gov.hmrc.agentclientmanagementfrontend.connectors

import uk.gov.hmrc.agentclientmanagementfrontend.stubs.AgentClientRelationshipsStub
import uk.gov.hmrc.agentclientmanagementfrontend.support.BaseISpec
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, Upstream5xxResponse}

import scala.concurrent.ExecutionContext.Implicits.global

class AgentClientRelationshipConnectorISpec extends BaseISpec with AgentClientRelationshipsStub {

  implicit val hc = HeaderCarrier()
  val connector = app.injector.instanceOf[AgentClientRelationshipsConnector]
  val arn = Arn("TARN0000001")
  val clientId = MtdItId("AA123456A")

  "Delete agent client relationships" should {
    "return true when a relationship has been deleted successfully" in {
      deleteActiveITSARelationship(arn.value, clientId.value)

      val result = await(connector.deleteRelationship(arn, clientId))
      result shouldBe true
    }

    "return false when a relationship is not found while deleting" in {
      deleteActiveITSARelationship(arn.value, clientId.value, 404)

      val result = await(connector.deleteRelationship(arn, clientId))
      result shouldBe false
    }

    "return exception when delete relationship throws bad request" in {
      deleteActiveITSARelationship(arn.value, clientId.value, 400)

      an[BadRequestException] should be thrownBy (await(connector.deleteRelationship(arn, clientId)))
    }

    "return exception when service returns bad gateway" in {
      deleteActiveITSARelationship(arn.value, clientId.value, 502)

      an[Upstream5xxResponse] should be thrownBy (await(connector.deleteRelationship(arn, clientId)))
    }

  }
}
