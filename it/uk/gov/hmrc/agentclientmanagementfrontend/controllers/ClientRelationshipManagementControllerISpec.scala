package uk.gov.hmrc.agentclientmanagementfrontend.controllers

import org.joda.time.LocalDate
import play.api.libs.ws._
import play.api.test.FakeRequest
import play.utils.UriEncoding
import uk.gov.hmrc.agentclientmanagementfrontend.models.ClientCache
import uk.gov.hmrc.agentclientmanagementfrontend.stubs._
import uk.gov.hmrc.agentclientmanagementfrontend.support.BaseISpec
import uk.gov.hmrc.agentclientmanagementfrontend.util.Services
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, Vrn}
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class ClientRelationshipManagementControllerISpec extends BaseISpec
  with PirRelationshipStub
  with AgentServicesAccountStub
  with AgentClientRelationshipsStub
  with AgentClientAuthorisationStub {

  override def featureRemoveAuthorisationPir = true
  override def featureRemoveAuthorisationITSA = true
  override def featureRemoveAuthorisationVat = true

  private lazy val controller: ClientRelationshipManagementController = app.injector.instanceOf[ClientRelationshipManagementController]
  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  private implicit val hc = HeaderCarrier(sessionId = Some(SessionId("sessionId123456")))
  val urlJustWithPrefix = s"http://localhost:$port/manage-your-tax-agents"
  val doGetRequest = (endOfUrl: String) => wsClient.url(s"$urlJustWithPrefix$endOfUrl").get()

  val mtdItId = MtdItId("ABCDEF123456789")
  val validArn = Arn("FARN0001132")
  val validNino =  Nino("AE123456A")
  val validVrn =  Vrn("101747641")
  val startDate = Some(LocalDate.parse("2017-06-06"))
  val startDateString = "2017-06-06"
  val encodedClientId = UriEncoding.encodePathSegment(mtdItId.value, "UTF-8")
  val cache = ClientCache("dc89f36b64c94060baa3ae87d6b7ac08", validArn, "This Agency Name", "Some service name", startDate)
  val cacheItsa = ClientCache("dc89f36b64c94060baa3ae87d6b7ac08", validArn, "This Agency Name", "HMRC-MTD-IT", startDate)
  val serviceItsa = Services.HMRCMTDIT
  val serviceVat = Services.HMRCMTDVAT
  val serviceIrv = Services.HMRCPIR

  "root" should {
    val req = FakeRequest()
    "redirect to home and show invitations tab if found invitations, including Pending invitations and active relationships" in {
      getClientActiveAgentRelationships(serviceItsa, validArn.value, startDateString)
      getClientActiveAgentRelationships(serviceVat, validArn.value, startDateString)
      getActivePIRRelationship( validArn, serviceIrv,validNino.value, false)
      getAgencyNameMap200(validArn, "My Boolean Agency")
      getThreeAgencyNamesMap200((validArn,"abc"),(validArn,"DEF"),(validArn, "ghi"))
      getInvitations(validArn, validNino.value, "NI", serviceIrv, "Pending", "9999-01-01")
      getInvitations(validArn, mtdItId.value, "MTDITID", serviceItsa, "Pending", "9999-01-01")
      getInvitations(validArn, validVrn.value, "VRN", serviceVat, "Pending", "9999-01-01")
      val result = controller.root()(authorisedAsClientAll(req, validNino.value, mtdItId.value, validVrn.value))
      status(result) shouldBe 303
      redirectLocation(result).get shouldBe "/manage-your-tax-agents/home#tabLinkRequests"
    }

    "redirect to home and show invitations tab if found invitations, including Pending invitations and no active relationships" in {
      getNotFoundClientActiveAgentRelationships(serviceItsa)
      getNotFoundClientActiveAgentRelationships(serviceVat)
      getNotFoundForPIRRelationship(serviceIrv, validNino.value)
      getAgencyNameMap200(validArn, "My Boolean Agency")
      getInvitations(validArn, validNino.value, "NI", serviceIrv, "Pending", "9999-01-01")
      getInvitations(validArn, mtdItId.value, "MTDITID", serviceItsa, "Pending", "9999-01-01")
      getInvitations(validArn, validVrn.value, "VRN", serviceVat, "Pending", "9999-01-01")
      val result = controller.root()(authorisedAsClientAll(req, validNino.value, mtdItId.value, validVrn.value))
      status(result) shouldBe 303
      redirectLocation(result).get shouldBe "/manage-your-tax-agents/home#tabLinkRequests"
    }

    "redirect to home and show invitations tab if found invitations but no relationships" in {
      getNotFoundClientActiveAgentRelationships(serviceItsa)
      getNotFoundClientActiveAgentRelationships(serviceVat)
      getNotFoundForPIRRelationship(serviceIrv, validNino.value)
      getAgencyNameMap200(validArn, "My Boolean Agency")
      getInvitations(validArn, validNino.value, "NI", serviceIrv, "Accepted", "9999-01-01")
      getInvitations(validArn, mtdItId.value, "MTDITID", serviceItsa, "Accepted", "9999-01-01")
      getInvitations(validArn, validVrn.value, "VRN", serviceVat, "Accepted", "9999-01-01")
      val result = controller.root()(authorisedAsClientAll(req, validNino.value, mtdItId.value, validVrn.value))
      status(result) shouldBe 303
      redirectLocation(result).get shouldBe "/manage-your-tax-agents/home#tabLinkRequests"
    }

    "redirect to home and show relationships tab if found relationships but no invitations" in {
      getClientActiveAgentRelationships(serviceItsa, validArn.value, startDateString)
      getClientActiveAgentRelationships(serviceVat, validArn.value, startDateString)
      getActivePIRRelationship( validArn, serviceIrv,validNino.value, false)
      getThreeAgencyNamesMap200((validArn,"abc"),(validArn,"DEF"),(validArn, "ghi"))
      getInvitationsNotFound(validNino.value, "NI")
      getInvitationsNotFound(mtdItId.value, "MTDITID")
      getInvitationsNotFound(validVrn.value, "VRN")
      val result = controller.root()(authorisedAsClientAll(req, validNino.value, mtdItId.value, validVrn.value))
      status(result) shouldBe 303
      redirectLocation(result).get shouldBe "/manage-your-tax-agents/home#tabLinkRelationships"
    }

    "redirect to home and show relationships tab if found no invitations or relationships" in {
      getNotFoundClientActiveAgentRelationships(serviceItsa)
      getNotFoundClientActiveAgentRelationships(serviceVat)
      getNotFoundForPIRRelationship(serviceIrv, validNino.value)
      getInvitationsNotFound(validNino.value, "NI")
      getInvitationsNotFound(mtdItId.value, "MTDITID")
      getInvitationsNotFound(validVrn.value, "VRN")
      val result = controller.root()(authorisedAsClientAll(req, validNino.value, mtdItId.value, validVrn.value))
      status(result) shouldBe 303
      redirectLocation(result).get shouldBe "/manage-your-tax-agents/home#tabLinkRelationships"
    }

    "redirect to home and show relationships tab if found invitations and relationships but no pending invitations" in {
      getClientActiveAgentRelationships(serviceItsa, validArn.value, startDateString)
      getClientActiveAgentRelationships(serviceVat, validArn.value, startDateString)
      getActivePIRRelationship( validArn, serviceIrv,validNino.value, false)
      getThreeAgencyNamesMap200((validArn,"abc"),(validArn,"DEF"),(validArn, "ghi"))
      getAgencyNameMap200(validArn, "My Boolean Agency")
      getInvitations(validArn, validNino.value, "NI", serviceIrv, "Accepted", "9999-01-01")
      getInvitations(validArn, mtdItId.value, "MTDITID", serviceItsa, "Accepted", "9999-01-01")
      getInvitations(validArn, validVrn.value, "VRN", serviceVat, "Accepted", "9999-01-01")
      val result = controller.root()(authorisedAsClientAll(req, validNino.value, mtdItId.value, validVrn.value))
      status(result) shouldBe 303
      redirectLocation(result).get shouldBe "/manage-your-tax-agents/home#tabLinkRelationships"
    }
  }

  "manageTaxAgents" should {
    val req = FakeRequest()

    "200, project authorised agent for a valid authenticated client with just PIR relationship and pending irv request" in {
      authorisedAsClientNi(req, validNino.nino)
      givenNinoIsKnownFor(validNino)
      getActivePIRRelationship(validArn, serviceIrv, validNino.value, fromCesa = false)
      getInvitations(validArn, validNino.value, "NI", serviceIrv, "Pending", "9999-01-01")
      getAgencyNameMap200(validArn, "This Agency Name")

      val result = await(doGetRequest(""))

      result.status shouldBe 200
      result.body.contains("This Agency Name") shouldBe true
      result.body.contains("View your PAYE income record") shouldBe true
      result.body.contains("Pending") shouldBe true
      result.body.contains("Expires: 01 January 9999") shouldBe true
      result.body.contains("Respond to request") shouldBe true
      result.body.contains("08 December 2017") shouldBe true
      result.body.contains("Remove authorisation") shouldBe true
      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "200, project authorised agent for a valid authenticated client with just Itsa relationship and expired itsa request" in {
      authorisedAsClientMtdItId(req, mtdItId.value)
      givenNinoIsKnownFor(validNino)
      getClientActiveAgentRelationships(serviceItsa, validArn.value, startDateString)
      getInvitations(validArn, mtdItId.value, "MTDITID", serviceItsa, "Expired", "9999-01-01")
      getAgencyNameMap200(validArn, "This Agency Name")

      val result = await(doGetRequest(""))

      result.status shouldBe 200
      result.body.contains("This Agency Name") shouldBe true
      result.body.contains("Report your income or expenses through software") shouldBe true
      result.body.contains("Expired") shouldBe true
      result.body.contains("01 January 9999") shouldBe true
      result.body.contains("No action needed") shouldBe true
      result.body.contains("06 June 2017") shouldBe true
      result.body.contains("Remove authorisation") shouldBe true
      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "200, project authorised agent for a valid authenticated client with just Vat relationship and rejected vat request" in {
      authorisedAsClientVat(req, validVrn.value)
      givenNinoIsKnownFor(validNino)
      getClientActiveAgentRelationships(serviceVat, validArn.value, startDateString)
      getInvitations(validArn, validVrn.value, "VRN", serviceVat, "Rejected", "9999-01-01")
      getAgencyNameMap200(validArn, "This Agency Name")

      val result = await(doGetRequest(""))

      result.status shouldBe 200
      result.body.contains("This Agency Name") shouldBe true
      result.body.contains("Report your VAT returns through software") shouldBe true
      result.body.contains("Declined") shouldBe true
      result.body.contains("15 January 2017") shouldBe true
      result.body.contains("No action needed") shouldBe true
      result.body.contains("06 June 2017") shouldBe true
      result.body.contains("Remove authorisation") shouldBe true
      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "200, project authorised agents in alphabetical order for valid authenticated client with ITSA, PIR and VAT relationships" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      givenNinoIsKnownFor(validNino)
      getClientActiveAgentRelationships(serviceItsa, validArn.value, startDateString)
      getActivePIRRelationship(validArn.copy(value="FARN0001131"), serviceIrv, validNino.value, fromCesa = false)
      getClientActiveAgentRelationships(serviceVat, validArn.copy(value="FARN0001133").value, startDateString)
      getThreeAgencyNamesMap200((validArn,"abc"),(validArn.copy(value="FARN0001131"),"DEF"),(validArn.copy(value = "FARN0001133"), "ghi"))
      getInvitations(validArn.copy(value="FARN0001133"), validVrn.value, "VRN", serviceVat, "Accepted", "9999-01-01")
      getInvitations(validArn, mtdItId.value, "MTDITID", serviceItsa, "Accepted", "9999-01-01")
      getInvitations(validArn.copy(value="FARN0001131"), validNino.value, "NI", serviceIrv, "Accepted", "9999-01-01")

      val result = await(doGetRequest(""))

      result.body.contains("No action needed") shouldBe true
      result.status shouldBe 200
      result.body.contains("abc") shouldBe true
      result.body.contains("DEF") shouldBe true
      result.body.contains("ghi") shouldBe true
      result.body.indexOf("abc") < result.body.indexOf("DEF") && result.body.indexOf("DEF")< result.body.indexOf("ghi") shouldBe true
      result.body.contains("Report your VAT returns through software") shouldBe true
      result.body.contains("Report your income or expenses through software") shouldBe true
      result.body.contains("View your PAYE income record") shouldBe true
      result.body.contains("15 January 2017") shouldBe true
      result.body.contains("07 March 2018") shouldBe false
      sessionStoreService.currentSession.clientCache.get.size == 3 shouldBe true
    }

    "200, no authorised agents message for valid authenticated client with no relationships" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      givenNinoIsKnownFor(validNino)
      getNotFoundClientActiveAgentRelationships(serviceItsa)
      getNotFoundForPIRRelationship(serviceIrv, validNino.value)
      getNotFoundClientActiveAgentRelationships(serviceVat)
      getThreeAgencyNamesMap200((validArn,"abc"),(validArn,"DEF"),(validArn, "ghi"))
      getInvitationsNotFound(validVrn.value, "VRN")
      getInvitationsNotFound(mtdItId.value, "MTDITID")
      getInvitationsNotFound(validNino.value, "NI")

      val result = await(doGetRequest(""))

      result.status shouldBe 200
      result.body.contains("You have no authorised agents") shouldBe true
      result.body.contains("You have no pending requests from tax agents") shouldBe true
      result.body.contains("Requests from agents") shouldBe true
      result.body.contains("Requests from agents <span class=\"badge\">0</span></span>") shouldBe false
      sessionStoreService.currentSession.clientCache.get.isEmpty shouldBe true
    }

    "200, project authorised agents when startDate is blank" in {
      authorisedAsClientMtdItId(req, mtdItId.value)
      givenNinoIsKnownFor(validNino)
      getClientActiveAgentRelationshipsNoStartDate(serviceItsa, validArn.value)
      getAgencyNameMap200(validArn, "This Agency Name")
      getInvitations(validArn, validVrn.value, "VRN", serviceVat, "Rejected", "9999-01-01")
      getInvitations(validArn, mtdItId.value, "MTDITID", serviceItsa, "Expired", "9999-01-01")
      getInvitations(validArn, validNino.value, "NI", serviceIrv, "Pending", "9999-01-01")

      val result = await(doGetRequest(""))

      result.status shouldBe 200
      result.body.contains("This Agency Name") shouldBe true
      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "200 project authorised agents with correct count of pending invitations" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      givenNinoIsKnownFor(validNino)
      getClientActiveAgentRelationships(serviceItsa, validArn.value, startDateString)
      getActivePIRRelationship(validArn.copy(value="FARN0001131"), serviceIrv, validNino.value, fromCesa = false)
      getClientActiveAgentRelationships(serviceVat, validArn.copy(value="FARN0001133").value, startDateString)
      getThreeAgencyNamesMap200((validArn,"abc"),(validArn.copy(value="FARN0001131"),"DEF"),(validArn.copy(value = "FARN0001133"), "ghi"))
      getInvitations(validArn.copy(value="FARN0001133"), validVrn.value, "VRN", serviceVat, "Pending", "9999-01-01")
      getInvitations(validArn, mtdItId.value, "MTDITID", serviceItsa, "Pending", "9999-01-01")
      getInvitations(validArn.copy(value="FARN0001131"), validNino.value, "NI", serviceIrv, "Pending", "9999-01-01")

      val result = await(doGetRequest(""))

      result.status shouldBe 200
      result.body.contains("Requests from agents <span class=\"badge\">3</span></span>") shouldBe true
    }

    "200 project authorised agents when requests are in sotre as pending but are actually expired" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      givenNinoIsKnownFor(validNino)
      getClientActiveAgentRelationships(serviceItsa, validArn.value, startDateString)
      getAgencyNameMap200(validArn,"abc")
      getInvitations(validArn, mtdItId.value, "MTDITID", serviceItsa, "Pending", "2017-01-01")

      val result = await(doGetRequest(""))

      result.status shouldBe 200
      result.body.contains("Expired") shouldBe true
      result.body.contains("01 January 2017") shouldBe true
      result.body.contains("No action needed") shouldBe true
      result.body.contains("Pending") shouldBe false
    }

    "500, when getAgencyNames in agent-services-account returns 400 invalid Arn" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      givenNinoIsKnownFor(validNino)
      getClientActiveAgentRelationships(serviceItsa, Arn("someInvalidArn").value, startDateString)
      getActivePIRRelationship(validArn, serviceIrv, validNino.value, fromCesa = false)
      getAgencyNamesMap400("someInvalidArn")

      val result = await(doGetRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "500, when getAgencyNames in agent-services-account returns 400 empty Arn" in {
      authorisedAsClientMtdItId(req, mtdItId.value)
      givenNinoIsKnownFor(validNino)
      getClientActiveAgentRelationships(serviceItsa, Arn("").value, startDateString)
      getActivePIRRelationship(validArn, serviceIrv, validNino.value, fromCesa = false)
      getAgencyNamesMap400("")

      val result = await(doGetRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "500, when Des returns 400" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      givenNinoIsKnownFor(validNino)
      get400ClientActiveAgentRelationships(serviceItsa)
      get400ClientActiveAgentRelationships(serviceVat)
      getActivePIRRelationship(validArn, serviceIrv, validNino.value, fromCesa = false)

      val result = await(doGetRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "500, when Des returns 500" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      givenNinoIsKnownFor(validNino)
      get500ClientActiveAgentRelationships(serviceItsa)
      get500ClientActiveAgentRelationships(serviceVat)
      getNotFoundForPIRRelationship(serviceIrv, validNino.value)

      val result = await(doGetRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "500, when Des returns 503" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      givenNinoIsKnownFor(validNino)
      get503ClientActiveAgentRelationships(serviceItsa)
      get503ClientActiveAgentRelationships(serviceVat)
      getActivePIRRelationship(validArn, serviceIrv, validNino.value, fromCesa = false)

      val result = await(doGetRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "500, when agent-fi-relationship returns 500" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      givenNinoIsKnownFor(validNino)
      getClientActiveAgentRelationships(serviceItsa, validArn.value, startDateString)
      getClientActiveAgentRelationships(serviceVat, validVrn.value, startDateString)
      get500ForPIRRelationship(serviceIrv, validNino.value)

      val result = await(doGetRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "500, when agent-fi-relationship returns 503" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      givenNinoIsKnownFor(validNino)
      getNotFoundClientActiveAgentRelationships(serviceItsa)
      getClientActiveAgentRelationships(serviceVat, validVrn.value, startDateString)
      get503ForPIRRelationship(serviceIrv, validNino.value)

      val result = await(doGetRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }
  }

  "showRemoveAuthorisation page" should {
    val req = FakeRequest()

    "return 200 OK and show remove authorisation page" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      sessionStoreService.storeClientCache(Seq(cache))

      val result = await(doGetRequest("/remove-authorisation/service/PERSONAL-INCOME-RECORD/id/dc89f36b64c94060baa3ae87d6b7ac08"))

      result.status shouldBe 200
      result.body.contains("This Agency Name") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe true
    }

    "return 500 exception when an invalid id is passed" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      sessionStoreService.storeClientCache(Seq(cache))

      val result = await(doGetRequest("/remove-authorisation/service/PERSONAL-INCOME-RECORD/id/INVALID_ID"))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
    }

    "return 500 exception when session cache not found" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)

      val result = await(doGetRequest("/remove-authorisation/service/PERSONAL-INCOME-RECORD/id/dc89f36b64c94060baa3ae87d6b7ac08"))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
    }
  }

  "removeAuthorisations for PERSONAL-INCOME-RECORD" should {

    behave like checkRemoveAuthorisationForService("PERSONAL-INCOME-RECORD", deleteActivePIRRelationship(validArn.value, validNino.value, 200))
    val req = FakeRequest()

    "return 500  an exception if PIR Relationship is not found" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceIrv)))
      deleteActivePIRRelationship(validArn.value, validNino.value, 404)

      an[Exception] should be thrownBy await(controller
        .submitRemoveAuthorisation(serviceIrv, "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "return an exception if PIR relationship service is unavailable" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceIrv)))
      deleteActivePIRRelationship(validArn.value, validNino.value, 500)

      an[Exception] should be thrownBy await(controller
        .submitRemoveAuthorisation(serviceIrv, "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value).withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "throw InsufficientEnrolments when Enrolment for chosen service is not found for logged in user" in {
      an[InsufficientEnrolments] shouldBe thrownBy {
        await(controller.submitRemoveAuthorisation("PERSONAL-INCOME-RECORD", "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientMtdItId(req, mtdItId.value).withFormUrlEncodedBody("confirmResponse" -> "true")))
      }
    }
  }

  "removeAuthorisations for ITSA" should {

    behave like checkRemoveAuthorisationForService(serviceItsa, deleteActiveITSARelationship(validArn.value, mtdItId.value, 204))
    val req = FakeRequest()

    "return 500 an exception if the relationship is not found" in {

      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceItsa)))
      deleteActiveITSARelationship(validArn.value, mtdItId.value, 404)

      an[Exception] should be thrownBy await(controller
        .submitRemoveAuthorisation(serviceItsa, "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value).withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "return an exception if relationship service is unavailable" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceItsa)))
      deleteActiveITSARelationship(validArn.value, mtdItId.value, 500)

      an[Exception] should be thrownBy await(controller
        .submitRemoveAuthorisation(serviceItsa, "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value).withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "throw InsufficientEnrolments when Enrolment for chosen service is not found for logged in user" in {
      an[InsufficientEnrolments] shouldBe thrownBy {
        await(controller.submitRemoveAuthorisation(serviceItsa, "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientNi(req, validNino.nino).withFormUrlEncodedBody("confirmResponse" -> "true")))
      }
    }
  }

  "removeAuthorisations for VAT" should {

    behave like checkRemoveAuthorisationForService(serviceVat, deleteActiveVATRelationship(validArn.value, validVrn.value, 204))
    val req = FakeRequest()

    "return 500  an exception if the relationship is not found" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceVat)))
      deleteActiveVATRelationship(validArn.value, validVrn.value, 404)

      an[Exception] should be thrownBy await(controller
        .submitRemoveAuthorisation(serviceVat, "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value).withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "return an exception if relationship service is unavailable" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceVat)))
      deleteActiveVATRelationship(validArn.value, validVrn.value, 500)

      an[Exception] should be thrownBy await(controller
        .submitRemoveAuthorisation(serviceVat, "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value).withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "throw InsufficientEnrolments when Enrolment for chosen service is not found for logged in user" in {
      an[InsufficientEnrolments] shouldBe thrownBy {
        await(controller.submitRemoveAuthorisation(serviceVat, "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientNi(req, validNino.nino).withFormUrlEncodedBody("confirmResponse" -> "true")))
      }
    }

    "return exception if session data not found" in {
      val req = FakeRequest().withSession("agencyName" -> cache.agencyName)
      an[Exception] should be thrownBy await(controller.submitRemoveAuthorisation(serviceItsa, "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)))
    }
  }

  "removeAuthorisations for invalid services" should {

    val req = FakeRequest()

    "return an exception because service is invalid" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = "InvalidService")))
      deleteActiveITSARelationship(validArn.value, mtdItId.value, 500)

      an[Exception] should be thrownBy await(controller
        .submitRemoveAuthorisation("InvalidService", "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value).withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }
  }

  "authorisationRemoved" should {

    "show authorisation_removed page with required sessions" in {
      val req = FakeRequest().withSession("agencyName" -> cacheItsa.agencyName, "service" -> cacheItsa.service)
      val result = await(controller.authorisationRemoved(authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)))

      status(result) shouldBe 200
      checkHtmlResultWithBodyText(result, "This Agency Name")
      checkHtmlResultWithBodyText(result, "cannot report your income and expenses through software")
    }

    "return exception if required session data not found" in {
      val req = FakeRequest().withSession("agencyName" -> cache.agencyName)
      an[Exception] should be thrownBy await(controller.authorisationRemoved(authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)))
    }
  }

  def checkRemoveAuthorisationForService(serviceName: String, deleteRelationshipStub: => Unit) = {
    implicit val req = FakeRequest()

    "return 200, remove the relationship if the client confirms deletion" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceName)))
      deleteRelationshipStub

      val result = await(controller.submitRemoveAuthorisation(serviceName, "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value).withFormUrlEncodedBody("confirmResponse" -> "true")))

      status(result) shouldBe 303
      sessionStoreService.currentSession.clientCache.get.isEmpty shouldBe true

      result.session should not be empty
      result.session.get("agencyName") shouldBe Some(cache.agencyName)
      result.session.get("service") shouldBe Some(serviceName)
    }

    "redirect to manage-your-tax-agents if the client does not confirm deletion" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceName)))
      deleteRelationshipStub

      val result = await(controller.submitRemoveAuthorisation(serviceName, "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value).withFormUrlEncodedBody("confirmResponse" -> "false")))

      status(result) shouldBe 303
      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "show error message if the client does not select any choice from the confirm delete radio buttons" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceName)))
      deleteRelationshipStub

      val result = await(controller.submitRemoveAuthorisation(serviceName, "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value).withFormUrlEncodedBody("confirmResponse" -> "")))

      status(result) shouldBe 200
      checkHtmlResultWithBodyText(result, "Select yes if you want to remove your authorisation.")
      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "return an exception if the session cache is not found" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      deleteRelationshipStub

      an[Exception] should be thrownBy await(controller
        .submitRemoveAuthorisation(serviceName, "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value).withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache shouldBe empty
    }

    "return an exception if an invalid id is submitted" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceName)))
      deleteRelationshipStub

      an[Exception] should be thrownBy await(controller
        .submitRemoveAuthorisation(serviceName, "INVALID_ID")(authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value).withFormUrlEncodedBody("confirmResponse" -> "true")))
    }

    "remove deleted item from the session cache" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceName),
        cache.copy(uuId = "dc89f36b64c94060baa3ae87d6b7ac09next", service = serviceName)))
      sessionStoreService.currentSession.clientCache.get.size == 2 shouldBe true
      deleteRelationshipStub

      val result = await(controller.submitRemoveAuthorisation(serviceName, "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value).withFormUrlEncodedBody("confirmResponse" -> "true")))

      status(result) shouldBe 303
      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
      sessionStoreService.currentSession.clientCache.get.head.uuId shouldBe "dc89f36b64c94060baa3ae87d6b7ac09next"
    }
  }
}
