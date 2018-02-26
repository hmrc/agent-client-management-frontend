package uk.gov.hmrc.agentclientmanagementfrontend.connectors

import play.utils.UriEncoding
import uk.gov.hmrc.agentclientmanagementfrontend.stubs.DesStub
import uk.gov.hmrc.agentclientmanagementfrontend.support.BaseISpec
import uk.gov.hmrc.agentclientmanagementfrontend.util.Services
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global

class DesConnectorISpec extends BaseISpec with DesStub {

  implicit val hc = HeaderCarrier()
  val connector = app.injector.instanceOf[DesConnector]
  val mtdItId = MtdItId("ABCDEF123456789")
  val agentARN = Arn("ABCDE123456")

  "Get client ITSA relationships from DES" should {
    val encodedClientId = UriEncoding.encodePathSegment(mtdItId.value, "UTF-8")

    "return existing active relationships for specified clientId" in {
      getClientActiveAgentRelationships(encodedClientId, Services.ITSA, agentARN.value)

      val result = await(connector.getActiveClientItsaRelationships(mtdItId))
      //result shouldBe defined
      result.get.arn shouldBe agentARN
    }

    "return notFound active relationships for specified clientId" in {
      getNotFoundClientActiveAgentRelationships(encodedClientId, Services.ITSA)

      an[Exception] should be thrownBy await(connector.getActiveClientItsaRelationships(mtdItId))
    }
  }
}