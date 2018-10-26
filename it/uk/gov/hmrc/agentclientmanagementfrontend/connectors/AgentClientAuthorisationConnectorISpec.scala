package uk.gov.hmrc.agentclientmanagementfrontend.connectors

import uk.gov.hmrc.agentclientmanagementfrontend.stubs.AgentClientAuthorisationStub
import uk.gov.hmrc.agentclientmanagementfrontend.support.BaseISpec
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, Vrn}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class AgentClientAuthorisationConnectorISpec extends BaseISpec with AgentClientAuthorisationStub {

  implicit val hc = HeaderCarrier()
  val connector = app.injector.instanceOf[AgentClientAuthorisationConnector]
  val arn = Arn("TARN0000001")
  val mtdItId = MtdItId("ABCDEF123456789")
  val nino = Nino("AB123456A")
  val vrn = Vrn("101747641")
  val serviceItsa = "HMRC-MTD-IT"
  val serviceVat = "HMRC-MTD-VAT"
  val startDate = "2007-07-07"
  val lastUpdated = "2017-01-15T13:14:00.000+08:00"

  "Get itsa invitations" should {
    "return existing invitations for specified clientId" in {
      getInvitations(arn, mtdItId.value, "MTDITID", "HMRC-MTD-IT", "Pending", "9999-01-01", lastUpdated)

      val result = await(connector.getItsaInvitation(mtdItId))
      result(0).arn shouldBe arn
      result(0).clientId shouldBe mtdItId.value
      result(0).invitationId shouldBe "ATDMZYN4YDLNW"
    }

    "return an empty sequence when no invitation is found for specified clientId" in {
      getInvitationsNotFound(mtdItId.value, "MTDITID")

      val result =  await(connector.getItsaInvitation(mtdItId))
      result shouldBe Seq.empty
    }
  }

  "Get irv invitations" should {
    "return existing invitations for specified clientId" in {
      getInvitations(arn, nino.value, "NI", "PERSONAL-INCOME-RECORD", "Pending", "9999-01-01", lastUpdated)

      val result = await(connector.getIrvInvitation(nino))
      result(0).arn shouldBe arn
      result(0).clientId shouldBe nino.value
      result(0).invitationId shouldBe "ATDMZYN4YDLNW"
    }

    "return an empty sequence when no invitation is found for specified clientId" in {
      getInvitationsNotFound(nino.value, "NI")

      val result =  await(connector.getIrvInvitation(nino))
      result shouldBe Seq.empty
    }
  }

  "Get vat invitations" should {
    "return existing invitations for specified clientId" in {
      getInvitations(arn, vrn.value, "VRN", "HMRC-MTD-VAT", "Pending", "9999-01-01", lastUpdated)

      val result = await(connector.getVatInvitation(vrn))
      result(0).arn shouldBe arn
      result(0).clientId shouldBe vrn.value
      result(0).invitationId shouldBe "ATDMZYN4YDLNW"
    }

    "return an empty sequence when no invitation is found for specified clientId" in {
      getInvitationsNotFound(vrn.value, "VRN")

      val result =  await(connector.getVatInvitation(vrn))
      result shouldBe Seq.empty
    }
  }
}
