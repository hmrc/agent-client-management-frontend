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

  "Current requests tab" should {
    val req = FakeRequest()

    "Show tab when pending requests are present with correct number of pending invitations" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      givenNinoIsKnownFor(validNino)
      getClientActiveAgentRelationships(serviceItsa, validArn.value, startDateString)
      getActivePIRRelationship(validArn.copy(value = "FARN0001131"), serviceIrv, validNino.value, fromCesa = false)
      getClientActiveAgentRelationships(serviceVat, validArn.copy(value = "FARN0001133").value, startDateString)
      getThreeAgencyNamesMap200((validArn, "abc"), (validArn.copy(value = "FARN0001131"), "DEF"), (validArn.copy(value = "FARN0001133"), "ghi"))
      getInvitations(validArn.copy(value = "FARN0001133"), validVrn.value, "VRN", serviceVat, "Pending", "9999-01-01")
      getInvitations(validArn, mtdItId.value, "MTDITID", serviceItsa, "Pending", "9999-01-01")
      getInvitations(validArn.copy(value = "FARN0001131"), validNino.value, "NI", serviceIrv, "Pending", "9999-01-01")

      val result = await(doGetRequest(""))

      result.status shouldBe 200
      result.body.contains("Manage who can deal with HMRC for you") shouldBe true
      result.body.contains("Current requests") shouldBe true
      result.body.contains("You have 3 requests you need to respond to.") shouldBe true
      result.body.contains("Who sent the request") shouldBe true
      result.body.contains("You need to respond by") shouldBe true
      result.body.contains("What you need to do") shouldBe true
      result.body.contains("01 January 9999") shouldBe true
      result.body.contains("abc") shouldBe true
      result.body.contains("Respond to request") shouldBe true
    }

    "Show tab with different message when number of pending invitations is 1" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      givenNinoIsKnownFor(validNino)
      getNotFoundClientActiveAgentRelationships(serviceItsa)
      getNotFoundForPIRRelationship(serviceIrv, validNino.value)
      getNotFoundClientActiveAgentRelationships(serviceVat)
      getAgencyNameMap200(validArn.copy(value = "FARN0001133"), "ghi")
      getInvitations(validArn.copy(value = "FARN0001133"), validVrn.value, "VRN", serviceVat, "Pending", "9999-01-01")
      getInvitationsNotFound(mtdItId.value, "MTDITID")
      getInvitationsNotFound(validNino.value, "NI")

      val result = await(doGetRequest(""))

      result.status shouldBe 200
      result.body.contains("Manage who can deal with HMRC for you") shouldBe true
      result.body.contains("Current requests") shouldBe true
      result.body.contains("You have 1 request you need to respond to.") shouldBe true
    }

    "Don't show tab when there are no pending invitations" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      givenNinoIsKnownFor(validNino)
      getNotFoundClientActiveAgentRelationships(serviceItsa)
      getNotFoundForPIRRelationship(serviceIrv, validNino.value)
      getNotFoundClientActiveAgentRelationships(serviceVat)
      getAgencyNameMap200(validArn.copy(value = "FARN0001133"), "ghi")
      getInvitationsNotFound(validVrn.value, "VRN")
      getInvitationsNotFound(mtdItId.value, "MTDITID")
      getInvitationsNotFound(validNino.value, "NI")

      val result = await(doGetRequest(""))

      result.status shouldBe 200
      result.body.contains("Manage who can deal with HMRC for you") shouldBe true
      result.body.contains("Current requests") shouldBe false
      result.body.contains("Who sent the request") shouldBe false
      result.body.contains("You need to respond by") shouldBe false
      result.body.contains("What you need to do") shouldBe false
    }
  }

  "Who can deal with HMRC for you tab" should {
    val req = FakeRequest()

    "Show tab with authorised agents" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      givenNinoIsKnownFor(validNino)
      getClientActiveAgentRelationships(serviceItsa, validArn.value, startDateString)
      getActivePIRRelationship(validArn.copy(value = "FARN0001131"), serviceIrv, validNino.value, fromCesa = false)
      getClientActiveAgentRelationships(serviceVat, validArn.copy(value = "FARN0001133").value, startDateString)
      getThreeAgencyNamesMap200((validArn, "abc"), (validArn.copy(value = "FARN0001131"), "DEF"), (validArn.copy(value = "FARN0001133"), "ghi"))
      getInvitationsNotFound(validVrn.value, "VRN")
      getInvitationsNotFound(mtdItId.value, "MTDITID")
      getInvitationsNotFound(validNino.value, "NI")

      val result = await(doGetRequest(""))

      result.status shouldBe 200
      result.body.contains("Manage who can deal with HMRC for you") shouldBe true
      result.body.contains("Who can deal with HMRC for you") shouldBe true
      result.body.contains("Find who you currently allow to deal with HMRC and remove your consent if you want to do so.") shouldBe true
      result.body.contains("Report your VAT returns through software") shouldBe true
      result.body.contains("Report your income and expenses through software") shouldBe true
      result.body.contains("View your PAYE income record") shouldBe true
      result.body.contains("abc") shouldBe true
      result.body.contains("6 June 2017") shouldBe true
      result.body.contains("Remove authorisation") shouldBe true
      sessionStoreService.currentSession.clientCache.get.size == 3 shouldBe true
    }

    "Show tab with no authorised agents and different content" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      givenNinoIsKnownFor(validNino)
      getNotFoundClientActiveAgentRelationships(serviceItsa)
      getNotFoundForPIRRelationship(serviceIrv, validNino.value)
      getNotFoundClientActiveAgentRelationships(serviceVat)
      getThreeAgencyNamesMap200((validArn, "abc"), (validArn.copy(value = "FARN0001131"), "DEF"), (validArn.copy(value = "FARN0001133"), "ghi"))
      getInvitationsNotFound(validVrn.value, "VRN")
      getInvitationsNotFound(mtdItId.value, "MTDITID")
      getInvitationsNotFound(validNino.value, "NI")

      val result = await(doGetRequest(""))

      result.status shouldBe 200
      println(result.body)
      result.body.contains("Manage who can deal with HMRC for you") shouldBe true
      result.body.contains("Who can deal with HMRC for you") shouldBe true
      result.body.contains("You have not appointed someone to deal with HMRC currently.") shouldBe true
    }

    "Show tab with authorised agents when startDate is blank" in {
      authorisedAsClientMtdItId(req, mtdItId.value)
      givenNinoIsKnownFor(validNino)
      getClientActiveAgentRelationshipsNoStartDate(serviceItsa, validArn.value)
      getAgencyNameMap200(validArn, "This Agency Name")
      getInvitationsNotFound(validVrn.value, "VRN")
      getInvitationsNotFound(mtdItId.value, "MTDITID")
      getInvitationsNotFound(validNino.value, "NI")

      val result = await(doGetRequest(""))

      result.status shouldBe 200
      println(result.body)
      result.body.contains("Manage who can deal with HMRC for you") shouldBe true
      result.body.contains("Find who you currently allow to deal with HMRC and remove your consent if you want to do so.") shouldBe true
      result.body.contains("This Agency Name") shouldBe true
      result.body.contains("Report your income and expenses through software") shouldBe true
      result.body.contains("Remove authorisation") shouldBe true
      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "500 when getAgencyNames in agent-services-account returns 400 invalid Arn" in {
      authorisedAsClientMtdItId(req, mtdItId.value)
      givenNinoIsKnownFor(validNino)
      getClientActiveAgentRelationships(serviceItsa, Arn("someInvalidArn").value, startDateString)
      getInvitationsNotFound(mtdItId.value, "MTDITID")
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
      getInvitationsNotFound(mtdItId.value, "MTDITID")
      getAgencyNamesMap400("")

      val result = await(doGetRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }
  }

  "manageTaxAgents Your activity history tab" should {
    val req = FakeRequest()

    "200 project MYTA page for a client with all services and different response scenarios in date order" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      givenNinoIsKnownFor(validNino)
      getNotFoundClientActiveAgentRelationships(serviceItsa)
      getNotFoundForPIRRelationship(serviceIrv, validNino.value)
      getNotFoundClientActiveAgentRelationships(serviceVat)
      getThreeAgencyNamesMap200((validArn,"abc"),(validArn.copy(value="FARN0001131"),"DEF"),(validArn.copy(value = "FARN0001133"), "ghi"))
      getInvitations(validArn.copy(value="FARN0001133"), validVrn.value, "VRN", serviceVat, "Accepted", "9999-01-01")
      getInvitations(validArn, mtdItId.value, "MTDITID", serviceItsa, "Rejected", "9999-01-01")
      getInvitations(validArn.copy(value="FARN0001131"), validNino.value, "NI", serviceIrv, "Expired", "9999-01-01")

      val result = await(doGetRequest(""))

      result.status shouldBe 200
      result.body.contains("abc") shouldBe true
      result.body.contains("DEF") shouldBe true
      result.body.contains("ghi") shouldBe true
      result.body.indexOf("DEF") < result.body.indexOf("abc") && result.body.indexOf("abc")< result.body.indexOf("ghi") shouldBe true
      result.body.contains("Report your income and expenses through software") shouldBe true
      result.body.contains("View your PAYE income record") shouldBe true
      result.body.contains("Report your VAT returns through software") shouldBe true
      result.body.contains("You accepted this request") shouldBe true
      result.body.contains("This request expired before you responded") shouldBe true
      result.body.contains("15 January 2017") shouldBe true
      result.body.contains("01 January 9999") shouldBe true
    }

    "200 project MYTA page for a client with all services and different response scenarios in alphabetical order when dates are the same" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      givenNinoIsKnownFor(validNino)
      getNotFoundClientActiveAgentRelationships(serviceItsa)
      getNotFoundForPIRRelationship(serviceIrv, validNino.value)
      getNotFoundClientActiveAgentRelationships(serviceVat)
      getThreeAgencyNamesMap200((validArn,"abc"),(validArn.copy(value="FARN0001131"),"def"),(validArn.copy(value = "FARN0001133"), "ghi"))
      getInvitations(validArn.copy(value="FARN0001133"), validVrn.value, "VRN", serviceVat, "Accepted", "9999-01-01")
      getInvitations(validArn, mtdItId.value, "MTDITID", serviceItsa, "Rejected", "9999-01-01")
      getInvitations(validArn.copy(value="FARN0001131"), validNino.value, "NI", serviceIrv, "Expired", "2017-01-15")

      val result = await(doGetRequest(""))

      result.status shouldBe 200
      result.body.contains("abc") shouldBe true
      result.body.contains("def") shouldBe true
      result.body.contains("ghi") shouldBe true
      result.body.indexOf("abc") < result.body.indexOf("def") && result.body.indexOf("def")< result.body.indexOf("ghi") shouldBe true
      result.body.contains("Report your income and expenses through software") shouldBe true
      result.body.contains("View your PAYE income record") shouldBe true
      result.body.contains("Report your VAT returns through software") shouldBe true
      result.body.contains("You accepted this request") shouldBe true
      result.body.contains("This request expired before you responded") shouldBe true
      result.body.contains("15 January 2017") shouldBe true
    }

    "200 project MYTA page for client with no relationship history" in {
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
      result.body.contains("Your activity history") shouldBe true
      result.body.contains("Find details of previous requests from agents.") shouldBe true
      result.body.contains("You do not have any previous activity.") shouldBe true
      sessionStoreService.currentSession.clientCache.get.isEmpty shouldBe true
    }

    "200 project MYTA page for client when requests are in store as pending but are actually expired" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value)
      givenNinoIsKnownFor(validNino)
      getNotFoundClientActiveAgentRelationships(serviceItsa)
      getNotFoundForPIRRelationship(serviceIrv, validNino.value)
      getNotFoundClientActiveAgentRelationships(serviceVat)
      getAgencyNameMap200(validArn, "This Agency Name")
      getInvitations(validArn, mtdItId.value, "MTDITID", serviceItsa, "Pending", "2017-01-01")
      getInvitationsNotFound(validVrn.value, "VRN")
      getInvitationsNotFound(validNino.value, "NI")

      val result = await(doGetRequest(""))

      result.status shouldBe 200
      result.body.contains("This request expired before you responded") shouldBe true
      result.body.contains("01 January 2017") shouldBe true
      result.body.contains("Pending") shouldBe false
    }

    "500, when Des returns 400" in {
      authorisedAsClientMtdItId(req, mtdItId.value)
      givenNinoIsKnownFor(validNino)
      get400ClientActiveAgentRelationships(serviceItsa)
      getInvitationsNotFound(mtdItId.value, "MTDITID")

      val result = await(doGetRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "500, when Des returns 500" in {
      authorisedAsClientMtdItId(req, mtdItId.value)
      givenNinoIsKnownFor(validNino)
      get500ClientActiveAgentRelationships(serviceItsa)
      getInvitationsNotFound(mtdItId.value, "MTDITID")

      val result = await(doGetRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "500, when Des returns 503" in {
      authorisedAsClientMtdItId(req, mtdItId.value)
      givenNinoIsKnownFor(validNino)
      get503ClientActiveAgentRelationships(serviceItsa)
      getInvitationsNotFound(mtdItId.value, "MTDITID")

      val result = await(doGetRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "500, when agent-fi-relationship returns 500" in {
      authorisedAsClientNi(req, validNino.nino)
      givenNinoIsKnownFor(validNino)
      get500ForPIRRelationship(serviceIrv, validNino.value)
      getInvitationsNotFound(validNino.value, "NI")

      val result = await(doGetRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "500, when agent-fi-relationship returns 503" in {
      authorisedAsClientNi(req, validNino.nino)
      givenNinoIsKnownFor(validNino)
      get503ForPIRRelationship(serviceIrv, validNino.value)
      getInvitationsNotFound(validNino.value, "NI")

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
