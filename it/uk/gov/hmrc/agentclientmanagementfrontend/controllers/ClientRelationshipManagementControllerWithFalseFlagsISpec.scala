package uk.gov.hmrc.agentclientmanagementfrontend.controllers

import org.joda.time.LocalDate
import play.api.libs.ws.WSClient
import play.api.test.FakeRequest
import play.utils.UriEncoding
import uk.gov.hmrc.agentclientmanagementfrontend.models.ClientCache
import uk.gov.hmrc.agentclientmanagementfrontend.stubs.{AgentClientRelationshipsStub, AgentServicesAccountStub, PirRelationshipStub}
import uk.gov.hmrc.agentclientmanagementfrontend.support.BaseISpec
import uk.gov.hmrc.agentclientmanagementfrontend.util.Services
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, Vrn}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId

import scala.concurrent.ExecutionContext.Implicits.global

class ClientRelationshipManagementControllerWithFalseFlagsISpec extends BaseISpec
  with PirRelationshipStub
  with AgentServicesAccountStub
  with AgentClientRelationshipsStub {

  override def featureRemoveAuthorisationPir = false

  override def featureRemoveAuthorisationITSA = false

  override def featureRemoveAuthorisationVat = false

  private lazy val controller: ClientRelationshipManagementController = app.injector.instanceOf[ClientRelationshipManagementController]
  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  private implicit val hc = HeaderCarrier(sessionId = Some(SessionId("sessionId123456")))
  val urlJustWithPrefix = s"http://localhost:$port/manage-your-tax-agents"
  val doGetRequest = (endOfUrl: String) => wsClient.url(s"$urlJustWithPrefix$endOfUrl").get()

  val req = FakeRequest()
  val mtdItId = MtdItId("ABCDEF123456789")
  val validArn = Arn("FARN0001132")
  val validNino = Nino("AE123456A")
  val validVrn =  Vrn("101747641")
  val startDate = Some(LocalDate.parse("2017-06-06"))
  val startDateString = "2017-06-06"
  val serviceItsa = Services.HMRCMTDIT
  val serviceVat = Services.HMRCMTDVAT
  val serviceIrv = Services.HMRCPIR
  val encodedClientId = UriEncoding.encodePathSegment(mtdItId.value, "UTF-8")
  val cache = ClientCache("dc89f36b64c94060baa3ae87d6b7ac08", validArn, "This Agency Name", "Some service name", startDate)

  "manageTaxAgents, works as normal except projections of remove authorisation links for false service flag" should {
    "200, do not show remove authorisation links, other than that works normal" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      givenNinoIsKnownFor(validNino)
      getClientActiveAgentRelationships(serviceItsa, validArn.value, startDateString)
      getActivePIRRelationship(validArn.copy(value = "FARN0001131"), serviceIrv, validNino.value, fromCesa = false)
      getClientActiveAgentRelationships(serviceVat, validVrn.value, startDateString)
      getThreeAgencyNamesMap200((validArn, "abc"), (validArn.copy(value = "FARN0001131"), "DEF"), (validArn.copy(value = "FARN0001133"), "DEF"))

      val result = await(doGetRequest(""))
      result.body.contains("Remove authorisation") shouldBe false
    }
  }

  "view removeAuthorisation" should {
    behave like getRemoveAuthorisationPage(serviceIrv)
    behave like getRemoveAuthorisationPage(serviceItsa)

    def getRemoveAuthorisationPage(service: String) = {
      s"return BadRequest for service: $service when flag for service is false" in {
        authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)

        val result = await(doGetRequest(s"/remove-authorisation/service/$service/id/${cache.uuId}"))

        result.status shouldBe 400
      }
    }
  }

  "post removeAuthorisation, BadRequest as flags are false" should {
    behave like postRemoveAuthorisationForm(serviceItsa)
    behave like postRemoveAuthorisationForm(serviceIrv)

    def postRemoveAuthorisationForm(service: String) = {
      s"return BadRequest for attempting to remove relationship when flag for service: $service is false" in {
        authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)

        val result = await(controller.submitRemoveAuthorisation(service, cache.uuId)(authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value).withFormUrlEncodedBody("confirmResponse" -> "true")))

        status(result) shouldBe 400
      }
    }
  }
}
