package uk.gov.hmrc.agentclientmanagementfrontend.controllers

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import play.api.libs.ws._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmanagementfrontend.models.SuspensionDetails
import uk.gov.hmrc.agentclientmanagementfrontend.stubs._
import uk.gov.hmrc.agentclientmanagementfrontend.support.{BaseISpec, ClientRelationshipManagementControllerTestSetup}
import uk.gov.hmrc.agentmtdidentifiers.model._
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ClientRelationshipManagementControllerISpec
    extends BaseISpec with PirRelationshipStub with AgentClientRelationshipsStub
    with AgentClientAuthorisationStub with ClientRelationshipManagementControllerTestSetup {

  override def featureRemoveAuthorisationPir = true
  override def featureRemoveAuthorisationITSA = true
  override def featureRemoveAuthorisationVat = true
  override def featureRemoveAuthorisationTrust = true

  private lazy val controller: ClientRelationshipManagementController =
    app.injector.instanceOf[ClientRelationshipManagementController]
  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  private implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionId123456")))
  val urlJustWithPrefix = s"http://localhost:$port/manage-your-tax-agents"
  val doGetRequest: String => Future[WSResponse] = (endOfUrl: String) =>
    wsClient.url(s"$urlJustWithPrefix$endOfUrl").withFollowRedirects(false).get()

  "Current requests tab" should {
    "Show tab when pending requests are present with correct number of pending invitations" in new PendingInvitationsExist(
      3) with BaseTestSetUp with NoRelationshipsFound with NoSuspensions {

      val response: WSResponse = await(doGetRequest(""))

      response.status shouldBe 200
      checkResponseBodyWithText(
        response,
        "Manage who can deal with HMRC for you",
        "Current requests",
        "Who sent the request",
        "You need to respond by",
        "What you need to do",
        "1 January 9999",
        "abc",
        "Respond to request",
        "You have 3 requests you need to respond to."
      )
    }

    "Show tab with different message when number of pending invitations is 1" in
      new PendingInvitationsExist(1) with BaseTestSetUp with NoRelationshipsFound with NoSuspensions {
        val response: WSResponse = await(doGetRequest(""))

        response.status shouldBe 200
        checkResponseBodyWithText(
          response,
          "Manage who can deal with HMRC for you",
          "Current requests",
          "You have 1 request you need to respond to.")
      }

    "Don't show tab when there are no pending invitations" in new PendingInvitationsExist(0) with BaseTestSetUp
    with NoRelationshipsFound with NoSuspensions {
      val response: WSResponse = await(doGetRequest(""))

      response.status shouldBe 200
      checkResponseBodyWithText(
        response,
        "Manage who can deal with HMRC for you"
      )
      checkResponseBodyNotWithText(
        response,
        "Current requests",
        "Who sent the request",
        "You need to respond by",
        "What you need to do")
    }

    "Ignore invitation when there is no agent reference found for an Arn" in new PendingInvitationsExist(3)
    with BaseTestSetUp with NoSuspensions {
      givenAgentRefNotFoundFor(arn1)

      val result = await(doGetRequest(""))

      result.status shouldBe 200

      checkResponseBodyWithText(
        result,
        "You have 2 requests you need to respond to.") //out of 3 show only 2 in the UI due to one missing AgentRef
    }

    "not show a request when the invitation request is for a service for which the agent has subsequently been suspended" in new PendingInvitationsExist(
      3) with BaseTestSetUp with NoRelationshipsFound {

      givenSuspensionDetails(arn1.value, SuspensionDetails(suspensionStatus = true, Some(Set("VATC"))))
      givenSuspensionDetails(arn2.value, SuspensionDetails(suspensionStatus = false, None))
      givenSuspensionDetails(arn3.value, SuspensionDetails(suspensionStatus = false, None))

      val response: WSResponse = await(doGetRequest(""))

      response.status shouldBe 200
      checkResponseBodyWithText(
        response,
        "Manage who can deal with HMRC for you",
        "Current requests",
        "Who sent the request",
        "You need to respond by",
        "What you need to do",
        "1 January 9999",
        "DEF",
        "Respond to request",
        "You have 2 requests you need to respond to."
      )
    }

  }

  "Who can deal with HMRC for you tab" should {
    "Show tab with authorised agents" in new PendingInvitationsExist(0) with BaseTestSetUp with RelationshipsFound with NoSuspensions {
      val response: WSResponse = await(doGetRequest(""))

      response.status shouldBe 200
      checkResponseBodyWithText(
        response,
        "Manage who can deal with HMRC for you",
        "Who can deal with HMRC for you",
        "Find who you currently allow to deal with HMRC and remove your consent if you want to do so.",
        "Submit your VAT returns through software",
        "Send your Income Tax updates through software",
        "View your PAYE income record",
        "abc",
        "6 June 2017",
        "Remove authorisation"
      )
      sessionStoreService.currentSession.clientCache.get.size == 3 shouldBe true
    }

    "Show tab with no authorised agents and different content" in new PendingInvitationsExist(0) with BaseTestSetUp
    with NoRelationshipsFound {
      val response: WSResponse = await(doGetRequest(""))

      response.status shouldBe 200
      checkResponseBodyWithText(
        response,
        "Manage who can deal with HMRC for you",
        "Who can deal with HMRC for you",
        "You have not appointed someone to deal with HMRC currently.")
    }

    "Show tab with authorised agents when startDate is blank" in new PendingInvitationsExist(0) with BaseTestSetUp with NoSuspensions {
      getClientActiveAgentRelationshipsNoStartDate(serviceItsa, arn1.value)
      getAgencyNameMap200(arn1, "This Agency Name")

      val response: WSResponse = await(doGetRequest(""))

      response.status shouldBe 200
      checkResponseBodyWithText(
        response,
        "Manage who can deal with HMRC for you",
        "Find who you currently allow to deal with HMRC and remove your consent if you want to do so.",
        "This Agency Name",
        "Send your Income Tax updates through software",
        "Remove authorisation"
      )
      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "if suspension is enabled don't show suspended agents on this tab" in new PendingInvitationsExist(0) with BaseTestSetUp with RelationshipsFound {
      givenSuspensionDetails(arn1.value, SuspensionDetails(suspensionStatus = true, Some(Set("ITSA"))))
      givenSuspensionDetails(arn2.value, SuspensionDetails(suspensionStatus = false, None))
      givenSuspensionDetails(arn3.value, SuspensionDetails(suspensionStatus = false, None))

      val response: WSResponse = await(doGetRequest(""))

      response.status shouldBe 200
      checkResponseBodyWithText(
        response,
        "Manage who can deal with HMRC for you",
        "Who can deal with HMRC for you",
        "Find who you currently allow to deal with HMRC and remove your consent if you want to do so.",
        "Submit your VAT returns through software",
        "View your PAYE income record",
        "6 June 2017",
        "Remove authorisation"
      )
      val doc = Jsoup.parse(response.body)
      val currentAuthsTab: Elements = doc.select("section[id=\"currentAuths\"]")
      currentAuthsTab.contains("abc") shouldBe false
      currentAuthsTab.contains("Send your Income Tax updates through software") shouldBe false
      sessionStoreService.currentSession.clientCache.get.size == 3 shouldBe true
    }

    "500 when getAgencyNames in agent-client-authorisation returns 400 invalid Arn" in new BaseTestSetUp {
      getClientActiveAgentRelationships(serviceItsa, Arn("someInvalidArn").value, startDateString)
      getAgencyNamesMap400("someInvalidArn")

      val response: WSResponse = await(doGetRequest(""))

      response.status shouldBe 500
      checkResponseBodyWithText(response, "Sorry, there is a problem with the service")
      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "500, when getAgencyNames in agent-client-authorisation returns 400 empty Arn" in new BaseTestSetUp {
      getClientActiveAgentRelationships(serviceItsa, Arn("").value, startDateString)
      getAgencyNamesMap400("")

      val response = await(doGetRequest(""))

      response.status shouldBe 500
      checkResponseBodyWithText(response, "Sorry, there is a problem with the service")
      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }
  }

  "Your activity history tab" should {
    val req = FakeRequest()

    "Show tab for a client with all services and different response scenarios in date order" in new BaseTestSetUp
    with NoRelationshipsFound with InvitationHistoryExistsDifferentDates with NoSuspensions {
      val response = await(doGetRequest(""))

      response.status shouldBe 200
      checkResponseBodyWithText(
        response,
        "Your activity history",
        "Keep track of changes to who HMRC can deal with and find details of previous requests.",
        "abc",
        "DEF",
        "ghi",
        "Send your Income Tax updates through software",
        "View your PAYE income record",
        "Submit your VAT returns through software",
        "You accepted this request",
        "You declined this request",
        "This request expired before you responded",
        "15 January 2017",
        "5 January 2017"
      )
      checkResponseBodyNotWithText(response, "05 January 2017")
      response.body.indexOf("DEF") < response.body.indexOf("abc") && response.body.indexOf("abc") < response.body
        .indexOf("ghi") shouldBe true
    }

    "Show tab for a client with all services and different response scenarios in time order when dates are the same" in
      new BaseTestSetUp with NoRelationshipsFound with InvitationHistoryExistsDifferentTimes with NoSuspensions {
        val result = await(doGetRequest(""))

        result.status shouldBe 200

        result.body.indexOf("abc") < result.body.indexOf("DEF") && result.body.indexOf("DEF") < result.body.indexOf(
          "ghi") shouldBe true
      }

    "Show tab for a client with all services and different response scenarios in alphabetical order when dates are the same" in new BaseTestSetUp
    with NoRelationshipsFound with InvitationHistoryExistsDifferentNames with NoSuspensions {
      val result = await(doGetRequest(""))

      result.status shouldBe 200
      result.body.indexOf("abc") < result.body.indexOf("def") && result.body.indexOf("def") < result.body.indexOf("ghi") shouldBe true
    }

    "Show tab for a client with no relationship history" in new PendingInvitationsExist(0) with BaseTestSetUp with NoRelationshipsFound {
      val response = await(doGetRequest(""))

      response.status shouldBe 200
      checkResponseBodyWithText(response, "Your activity history", "You do not have any previous activity.")
      sessionStoreService.currentSession.clientCache.get.isEmpty shouldBe true
    }

    "500, when Des returns 400" in {
      authorisedAsClientMtdItId(req, mtdItId.value)
      get400ClientActiveAgentRelationships(serviceItsa)
      getInvitationsNotFound(mtdItId.value, "MTDITID")

      val result = await(doGetRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, there is a problem with the service") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "500, when Des returns 500" in {
      authorisedAsClientMtdItId(req, mtdItId.value)
      get500ClientActiveAgentRelationships(serviceItsa)
      getInvitationsNotFound(mtdItId.value, "MTDITID")

      val result = await(doGetRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, there is a problem with the service") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "500, when Des returns 503" in {
      authorisedAsClientMtdItId(req, mtdItId.value)
      get503ClientActiveAgentRelationships(serviceItsa)
      getInvitationsNotFound(mtdItId.value, "MTDITID")

      val result = await(doGetRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, there is a problem with the service") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "500, when agent-fi-relationship returns 500" in {
      authorisedAsClientNi(req, validNino.nino)
      get500ForPIRRelationship(serviceIrv, validNino.value)
      getInvitationsNotFound(validNino.value, "NI")

      val result = await(doGetRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, there is a problem with the service") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "500, when agent-fi-relationship returns 503" in {
      authorisedAsClientNi(req, validNino.nino)
      get503ForPIRRelationship(serviceIrv, validNino.value)
      getInvitationsNotFound(validNino.value, "NI")

      val result = await(doGetRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, there is a problem with the service") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }
  }

  "showRemoveAuthorisation page" should {
    val req = FakeRequest()

    "return 200 OK and show remove authorisation page" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
      sessionStoreService.storeClientCache(Seq(cache))

      val result =
        await(doGetRequest("/remove-authorisation/service/PERSONAL-INCOME-RECORD/id/dc89f36b64c94060baa3ae87d6b7ac08"))

      result.status shouldBe 200
      result.body.contains("This Agency Name") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe true
    }

    "return 200 OK and show remove authorisation page for trust" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
      sessionStoreService.storeClientCache(Seq(cache))

      val result =
        await(doGetRequest("/remove-authorisation/service/HMRC-TERS-ORG/id/dc89f36b64c94060baa3ae87d6b7ac08"))

      result.status shouldBe 200
      result.body.contains("This Agency Name") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe true
    }

    "redirect to /root when an invalid id is passed" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
      sessionStoreService.storeClientCache(Seq(cache))

      val result = await(doGetRequest("/remove-authorisation/service/PERSONAL-INCOME-RECORD/id/INVALID_ID"))
      result.status shouldBe 303
    }

    "redirect to /root when session cache not found" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)

      val result =
        await(doGetRequest("/remove-authorisation/service/PERSONAL-INCOME-RECORD/id/dc89f36b64c94060baa3ae87d6b7ac08"))

      result.status shouldBe 303
    }
  }

  "removeAuthorisations for PERSONAL-INCOME-RECORD" should {

    behave like checkRemoveAuthorisationForService(
      "PERSONAL-INCOME-RECORD",
      deleteActivePIRRelationship(arn1.value, validNino.value, 200))
    val req = FakeRequest()

    "return 500  an exception if PIR Relationship is not found" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceIrv)))
      deleteActivePIRRelationship(arn1.value, validNino.value, 404)

      an[Exception] should be thrownBy await(
        controller
          .submitRemoveAuthorisation(serviceIrv, "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientAll(
            req,
            validNino.nino,
            mtdItId.value,
            validVrn.value,
            validUtr.value,
            validCgtRef.value) withFormUrlEncodedBody ("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "return an exception if PIR relationship service is unavailable" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceIrv)))
      deleteActivePIRRelationship(arn1.value, validNino.value, 500)

      an[Exception] should be thrownBy await(
        controller
          .submitRemoveAuthorisation(serviceIrv, "dc89f36b64c94060baa3ae87d6b7ac08")(
            authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "throw InsufficientEnrolments when Enrolment for chosen service is not found for logged in user" in {
      an[InsufficientEnrolments] shouldBe thrownBy {
        await(
          controller.submitRemoveAuthorisation("PERSONAL-INCOME-RECORD", "dc89f36b64c94060baa3ae87d6b7ac08")(
            authorisedAsClientMtdItId(req, mtdItId.value).withFormUrlEncodedBody("confirmResponse" -> "true")))
      }
    }
  }

  "removeAuthorisations for ITSA" should {

    behave like checkRemoveAuthorisationForService(
      serviceItsa,
      deleteActiveITSARelationship(arn1.value, mtdItId.value, 204))
    val req = FakeRequest()

    "return 500 a runtime exception if the relationship is not found" in {

      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceItsa)))
      deleteActiveITSARelationship(arn1.value, mtdItId.value, 404)

      intercept[RuntimeException] {
          await(controller
            .submitRemoveAuthorisation(serviceItsa, "dc89f36b64c94060baa3ae87d6b7ac08")(
              authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
                .withFormUrlEncodedBody("confirmResponse" -> "true")))
      }.getMessage shouldBe "relationship deletion failed"

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "return an exception if relationship service is unavailable" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceItsa)))
      deleteActiveITSARelationship(arn1.value, mtdItId.value, 500)

      an[Exception] should be thrownBy await(
        controller
          .submitRemoveAuthorisation(serviceItsa, "dc89f36b64c94060baa3ae87d6b7ac08")(
            authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "throw InsufficientEnrolments when Enrolment for chosen service is not found for logged in user" in {
      an[InsufficientEnrolments] shouldBe thrownBy {
        await(
          controller.submitRemoveAuthorisation(serviceItsa, "dc89f36b64c94060baa3ae87d6b7ac08")(
            authorisedAsClientNi(req, validNino.nino).withFormUrlEncodedBody("confirmResponse" -> "true")))
      }
    }
  }

  "removeAuthorisations for VAT" should {

    behave like checkRemoveAuthorisationForService(
      serviceVat,
      deleteActiveVATRelationship(arn1.value, validVrn.value, 204))
    val req = FakeRequest()

    "return 500  an exception if the relationship is not found" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceVat)))
      deleteActiveVATRelationship(arn1.value, validVrn.value, 404)

      an[Exception] should be thrownBy await(
        controller
          .submitRemoveAuthorisation(serviceVat, "dc89f36b64c94060baa3ae87d6b7ac08")(
            authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "return an exception if relationship service is unavailable" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceVat)))
      deleteActiveVATRelationship(arn1.value, validVrn.value, 500)

      an[Exception] should be thrownBy await(
        controller
          .submitRemoveAuthorisation(serviceVat, "dc89f36b64c94060baa3ae87d6b7ac08")(
            authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "throw InsufficientEnrolments when Enrolment for chosen service is not found for logged in user" in {
      an[InsufficientEnrolments] shouldBe thrownBy {
        await(
          controller.submitRemoveAuthorisation(serviceVat, "dc89f36b64c94060baa3ae87d6b7ac08")(
            authorisedAsClientNi(req, validNino.nino).withFormUrlEncodedBody("confirmResponse" -> "true")))
      }
    }

    "return exception if session data not found" in {
      val req = FakeRequest().withSession("agencyName" -> cache.agencyName)
      val result = await(
        controller.submitRemoveAuthorisation(serviceItsa, "dc89f36b64c94060baa3ae87d6b7ac08")(
          authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)))
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.ClientRelationshipManagementController.root().url)
    }
  }

  "removeAuthorisations for Trust" should {

    behave like checkRemoveAuthorisationForService(
      serviceTrust,
      deleteActiveTrustRelationship(arn1.value, validUtr.value, 204))
    val req = FakeRequest()

    "return 500  an exception if the relationship is not found" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceTrust)))
      deleteActiveVATRelationship(arn1.value, validUtr.value, 404)

      an[Exception] should be thrownBy await(
        controller
          .submitRemoveAuthorisation(serviceTrust, "dc89f36b64c94060baa3ae87d6b7ac08")(
            authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "return an exception if relationship service is unavailable" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceTrust)))
      deleteActiveVATRelationship(arn1.value, validUtr.value, 500)

      an[Exception] should be thrownBy await(
        controller
          .submitRemoveAuthorisation(serviceVat, "dc89f36b64c94060baa3ae87d6b7ac08")(
            authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "throw InsufficientEnrolments when Enrolment for chosen service is not found for logged in user" in {
      an[InsufficientEnrolments] shouldBe thrownBy {
        await(
          controller.submitRemoveAuthorisation(serviceTrust, "dc89f36b64c94060baa3ae87d6b7ac08")(
            authorisedAsClientNi(req, validNino.nino).withFormUrlEncodedBody("confirmResponse" -> "true")))
      }
    }

    "return exception if session data not found" in {
      val req = FakeRequest().withSession("agencyName" -> cache.agencyName)
      val result = await(
        controller.submitRemoveAuthorisation(serviceItsa, "dc89f36b64c94060baa3ae87d6b7ac08")(
          authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)))
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.ClientRelationshipManagementController.root().url)
    }
  }

  "removeAuthorisations for invalid services" should {

    val req = FakeRequest()

    "return an exception because service is invalid" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = "InvalidService")))
      deleteActiveITSARelationship(arn1.value, mtdItId.value, 500)

      an[Exception] should be thrownBy await(
        controller
          .submitRemoveAuthorisation("InvalidService", "dc89f36b64c94060baa3ae87d6b7ac08")(
            authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }
  }

  "authorisationRemoved" should {

    "show authorisation_removed page with required sessions" in {
      val req = FakeRequest().withSession("agencyName" -> cacheItsa.agencyName, "service" -> cacheItsa.service)
      val result = await(
        controller.authorisationRemoved(
          authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)))

      status(result) shouldBe 200
      checkHtmlResultWithBodyText(
        result,
        "You removed your authorisation from This Agency Name to send your Income Tax updates through software")
    }

    "return exception if required session data not found" in {
      val req = FakeRequest().withSession("agencyName" -> cache.agencyName)
      val result = await(
        controller.authorisationRemoved(
          authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)))
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.ClientRelationshipManagementController.root().url)
    }
  }

  def checkRemoveAuthorisationForService(serviceName: String, deleteRelationshipStub: => StubMapping) = {
    implicit val req = FakeRequest()

    "return 200, remove the relationship if the client confirms deletion" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceName)))
      deleteRelationshipStub

      val result = await(
        controller.submitRemoveAuthorisation(serviceName, "dc89f36b64c94060baa3ae87d6b7ac08")(
          authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
            .withFormUrlEncodedBody("confirmResponse" -> "true")))

      status(result) shouldBe 303
      sessionStoreService.currentSession.clientCache.get.isEmpty shouldBe true

      result.session should not be empty
      result.session.get("agencyName") shouldBe Some(cache.agencyName)
      result.session.get("service") shouldBe Some(serviceName)
    }

    "redirect to manage-your-tax-agents if the client does not confirm deletion" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceName)))
      deleteRelationshipStub

      val result = await(
        controller.submitRemoveAuthorisation(serviceName, "dc89f36b64c94060baa3ae87d6b7ac08")(
          authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
            .withFormUrlEncodedBody("confirmResponse" -> "false")))

      status(result) shouldBe 303
      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "show error message if the client does not select any choice from the confirm delete radio buttons" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceName)))
      deleteRelationshipStub

      val result = await(
        controller.submitRemoveAuthorisation(serviceName, "dc89f36b64c94060baa3ae87d6b7ac08")(
          authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
            .withFormUrlEncodedBody("confirmResponse" -> "")))

      status(result) shouldBe 200
      checkHtmlResultWithBodyText(result, "Select yes if you want to remove your authorisation.")
      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "return an exception if the session cache is not found" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
      deleteRelationshipStub

      an[Exception] should be thrownBy await(
        controller
          .submitRemoveAuthorisation(serviceName, "dc89f36b64c94060baa3ae87d6b7ac08")(
            authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache shouldBe empty
    }

    "redirect to /root if an invalid id is submitted" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceName)))
      deleteRelationshipStub

      an[Exception] should be thrownBy await(
        controller
          .submitRemoveAuthorisation(serviceName, "INVALID_ID")(
            authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))
    }

    "remove deleted item from the session cache" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
      sessionStoreService.storeClientCache(
        Seq(
          cache.copy(service = serviceName),
          cache.copy(uuId = "dc89f36b64c94060baa3ae87d6b7ac09next", service = serviceName)))
      sessionStoreService.currentSession.clientCache.get.size == 2 shouldBe true
      deleteRelationshipStub

      val result = await(
        controller.submitRemoveAuthorisation(serviceName, "dc89f36b64c94060baa3ae87d6b7ac08")(
          authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
            .withFormUrlEncodedBody("confirmResponse" -> "true")))

      status(result) shouldBe 303
      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
      sessionStoreService.currentSession.clientCache.get.head.uuId shouldBe "dc89f36b64c94060baa3ae87d6b7ac09next"
    }
  }

  "timedOut" should {
    "display the timed out page" in {
      val response: WSResponse = await(doGetRequest("/timed-out"))
      response.status shouldBe 403
      checkResponseBodyWithText(response, "You have been signed out", "so we have signed you out to keep your account secure.")
    }
  }
}
