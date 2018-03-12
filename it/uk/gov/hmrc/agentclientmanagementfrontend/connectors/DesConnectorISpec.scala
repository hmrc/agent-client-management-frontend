package uk.gov.hmrc.agentclientmanagementfrontend.connectors

import play.utils.UriEncoding
import uk.gov.hmrc.agentclientmanagementfrontend.stubs.DesStub
import uk.gov.hmrc.agentclientmanagementfrontend.support.{BaseISpec, MetricsTestSupport}
import uk.gov.hmrc.agentclientmanagementfrontend.util.Services
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class DesConnectorISpec extends BaseISpec with DesStub with MetricsTestSupport {

  implicit val hc = HeaderCarrier()
  val connector = app.injector.instanceOf[DesConnector]
  val mtdItId = MtdItId("ABCDEF123456789")
  val agentARN = Arn("ABCDE123456")
  val nino = Nino("AB123456C")

  "DesConnector GetStatusAgentRelationship" should {
    val encodedClientId = UriEncoding.encodePathSegment(mtdItId.value, "UTF-8")

    "return existing active relationships for specified clientId" in {
      getClientActiveAgentRelationships(encodedClientId, Services.ITSA, agentARN.value)

      val result = await(connector.getActiveClientItsaRelationships(mtdItId))
      result.get.arn shouldBe agentARN
    }

    "return notFound active relationships for specified clientId" in {
      getNotFoundClientActiveAgentRelationships(encodedClientId, Services.ITSA)

      val result =  await(connector.getActiveClientItsaRelationships(mtdItId))
      result shouldBe None
    }

    "record metrics for GetStatusAgentRelationship" in {
      getClientActiveAgentRelationships(encodedClientId, Services.ITSA, agentARN.value)

      val result = await(connector.getActiveClientItsaRelationships(mtdItId))
      result.get.arn shouldBe agentARN
      verifyTimerExistsAndBeenUpdated("ConsumedAPI-DES-GetStatusAgentRelationship-GET")
    }
  }

  "DesConnector GetRegistrationBusinessDetails" should {
    "return some nino when agent's mtdbsa identifier is known to ETMP" in {
      givenNinoIsKnownFor(mtdItId, nino)
      givenAuditConnector()
      await(connector.getNinoFor(mtdItId)) shouldBe nino
    }

    "return nothing when agent's mtdbsa identifier is unknown to ETMP" in {
      givenNinoIsUnknownFor(mtdItId)
      givenAuditConnector()
      an[Exception] should be thrownBy await(connector.getNinoFor(mtdItId))
    }

    "fail when agent's mtdbsa identifier is invalid" in {
      givenMtdbsaIsInvalid(mtdItId)
      givenAuditConnector()
      an[Exception] should be thrownBy await(connector.getNinoFor(mtdItId))
    }

    "fail when DES is unavailable" in {
      givenDesReturnsServiceUnavailable()
      givenAuditConnector()
      an[Exception] should be thrownBy await(connector.getNinoFor(mtdItId))
    }

    "fail when DES is throwing errors" in {
      givenDesReturnsServerError()
      givenAuditConnector()
      an[Exception] should be thrownBy await(connector.getNinoFor(mtdItId))
    }

    "record metrics for GetRegistrationBusinessDetailsByMtdbsa" in {
      givenNinoIsKnownFor(mtdItId, Nino("AB123456C"))
      givenCleanMetricRegistry()
      givenAuditConnector()
      await(connector.getNinoFor(mtdItId))
      verifyTimerExistsAndBeenUpdated("ConsumedAPI-DES-GetRegistrationBusinessDetailsByMtdbsa-GET")
    }
  }
}