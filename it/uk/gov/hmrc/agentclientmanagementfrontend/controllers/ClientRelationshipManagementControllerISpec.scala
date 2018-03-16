package uk.gov.hmrc.agentclientmanagementfrontend.controllers

import play.api.libs.ws._
import play.api.test.FakeRequest
import play.utils.UriEncoding
import uk.gov.hmrc.agentclientmanagementfrontend.models.ClientCache
import uk.gov.hmrc.agentclientmanagementfrontend.stubs._
import uk.gov.hmrc.agentclientmanagementfrontend.support.BaseISpec
import uk.gov.hmrc.agentclientmanagementfrontend.util.Services
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}
import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId

import scala.concurrent.ExecutionContext.Implicits.global

class ClientRelationshipManagementControllerISpec extends BaseISpec
  with PirRelationshipStub
  with AgentServicesAccountStub
  with AgentClientRelationshipsStub {

  private lazy val controller: ClientRelationshipManagementController = app.injector.instanceOf[ClientRelationshipManagementController]
  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  private implicit val hc = HeaderCarrier(sessionId = Some(SessionId("sessionId123456")))
  val urlJustWithPrefix = s"http://localhost:$port/manage-your-tax-agents"
  val doGetRequest = (endOfUrl: String) => wsClient.url(s"$urlJustWithPrefix$endOfUrl").get()

  val mtdItId = MtdItId("ABCDEF123456789")
  val validArn = Arn("FARN0001132")
  val validNino =  Nino("AE123456A")
  val encodedClientId = UriEncoding.encodePathSegment(mtdItId.value, "UTF-8")
  val cache = ClientCache("dc89f36b64c94060baa3ae87d6b7ac08", validArn, "This Agency Name", "Some service name")

  "manageTaxAgents" should {
    val req = FakeRequest()

    "200, project authorised agent for a valid authenticated client with just PIR relationship" in {
      authorisedAsClientNi(req, validNino.nino)
      givenNinoIsKnownFor(validNino)
      getActivePIRRelationship(validArn, Services.HMRCPIR, validNino.value, fromCesa = false)
      getAgencyNameMap200(validArn, "This Agency Name")

      val result = await(doGetRequest(""))

      result.status shouldBe 200
      result.body.contains("This Agency Name") shouldBe true
      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "200, project authorised agent for a valid authenticated client with just Itsa relationship" in {
      authorisedAsClientMtdItId(req, mtdItId.value)
      givenNinoIsKnownFor(validNino)
      getClientActiveAgentRelationships(validArn.value)
      getAgencyNameMap200(validArn, "This Agency Name")

      val result = await(doGetRequest(""))

      result.status shouldBe 200
      result.body.contains("This Agency Name") shouldBe true
      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "200, project authorised agents in alphabetical order for valid authenticated client with ITSA and PIR relationship" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value)
      givenNinoIsKnownFor(validNino)
      getClientActiveAgentRelationships(validArn.value)
      getActivePIRRelationship(validArn.copy(value="FARN0001131"), Services.HMRCPIR, validNino.value, fromCesa = false)
      getTwoAgencyNamesMap200((validArn,"This Agency Name"),(validArn.copy(value="FARN0001131"),"Different"))

      val result = await(doGetRequest(""))

      result.status shouldBe 200
      result.body.contains("This Agency Name") shouldBe true
      result.body.contains("Different") shouldBe true
      result.body.indexOf("Different") < result.body.indexOf("This Agency Name") shouldBe true
      sessionStoreService.currentSession.clientCache.get.size == 2 shouldBe true
    }

    "200, no authorised agents message for valid authenticated client with no relationships" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value)
      givenNinoIsKnownFor(validNino)
      getNotFoundClientActiveAgentRelationships
      getNotFoundForPIRRelationship(Services.HMRCPIR, validNino.value)

      val result = await(doGetRequest(""))

      result.status shouldBe 200
      result.body.contains("You have no authorised agents") shouldBe true
      sessionStoreService.currentSession.clientCache.get.isEmpty shouldBe true
    }

    "500, when getAgencyNames in agent-services-account returns 400 invalid Arn" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value)
      givenNinoIsKnownFor(validNino)
      getClientActiveAgentRelationships(Arn("someInvalidArn").value)
      getActivePIRRelationship(validArn, Services.HMRCPIR, validNino.value, fromCesa = false)
      getAgencyNamesMap400("someInvalidArn")

      val result = await(doGetRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "500, when getAgencyNames in agent-services-account returns 400 empty Arn" in {
      authorisedAsClientMtdItId(req, mtdItId.value)
      givenNinoIsKnownFor(validNino)
      getClientActiveAgentRelationships(Arn("").value)
      getActivePIRRelationship(validArn, Services.HMRCPIR, validNino.value, fromCesa = false)
      getAgencyNamesMap400("")

      val result = await(doGetRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "500, when Des returns 400" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value)
      givenNinoIsKnownFor(validNino)
      get400ClientActiveAgentRelationships
      getActivePIRRelationship(validArn, Services.HMRCPIR, validNino.value, fromCesa = false)

      val result = await(doGetRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "500, when Des returns 500" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value)
      givenNinoIsKnownFor(validNino)
      get500ClientActiveAgentRelationships
      getNotFoundForPIRRelationship(Services.HMRCPIR, validNino.value)

      val result = await(doGetRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "500, when Des returns 503" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value)
      givenNinoIsKnownFor(validNino)
      get503ClientActiveAgentRelationships
      getActivePIRRelationship(validArn, Services.HMRCPIR, validNino.value, fromCesa = false)

      val result = await(doGetRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "500, when agent-fi-relationship returns 500" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value)
      givenNinoIsKnownFor(validNino)
      getClientActiveAgentRelationships(validArn.value)
      get500ForPIRRelationship(Services.HMRCPIR, validNino.value)

      val result = await(doGetRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "500, when agent-fi-relationship returns 503" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value)
      givenNinoIsKnownFor(validNino)
      getNotFoundClientActiveAgentRelationships
      get503ForPIRRelationship(Services.HMRCPIR, validNino.value)

      val result = await(doGetRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }
  }

  "showRemoveAuthorisation page" should {
    val req = FakeRequest()

    "return 200 OK and show remove authorisation page" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value)
      sessionStoreService.storeClientCache(Seq(cache))

      val result = await(doGetRequest("/remove-authorisation/service/PERSONAL-INCOME-RECORD/id/dc89f36b64c94060baa3ae87d6b7ac08"))

      result.status shouldBe 200
      result.body.contains("This Agency Name") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe true
    }

    "return 500 exception when an invalid id is passed" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value)
      sessionStoreService.storeClientCache(Seq(cache))

      val result = await(doGetRequest("/remove-authorisation/service/PERSONAL-INCOME-RECORD/id/INVALID_ID"))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
    }

    "return 500 exception when session cache not found" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value)

      val result = await(doGetRequest("/remove-authorisation/service/PERSONAL-INCOME-RECORD/id/dc89f36b64c94060baa3ae87d6b7ac08"))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
    }
  }

  "removeAuthorisations for PERSONAL-INCOME-RECORD" should {

    behave like checkRemoveAuthorisationForService("PERSONAL-INCOME-RECORD", deleteActivePIRRelationship(validArn.value, validNino.value, 200))
    val req = FakeRequest()

    "return 500  an exception if PIR Relationship is not found" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = "PERSONAL-INCOME-RECORD")))
      deleteActivePIRRelationship(validArn.value, validNino.value, 404)

      an[Exception] should be thrownBy await(controller
        .submitRemoveAuthorisation("PERSONAL-INCOME-RECORD", "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientAll(req, validNino.nino, mtdItId.value)withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "return an exception if PIR relationship service is unavailable" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = "PERSONAL-INCOME-RECORD")))
      deleteActivePIRRelationship(validArn.value, validNino.value, 500)

      an[Exception] should be thrownBy await(controller
        .submitRemoveAuthorisation("PERSONAL-INCOME-RECORD", "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientAll(req, validNino.nino, mtdItId.value).withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "throw InsufficientEnrolments when Enrolment for chosen service is not found for logged in user" in {
      an[InsufficientEnrolments] shouldBe thrownBy {
        await(controller.submitRemoveAuthorisation("PERSONAL-INCOME-RECORD", "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientMtdItId(req, mtdItId.value).withFormUrlEncodedBody("confirmResponse" -> "true")))
      }
    }
  }

  "removeAuthorisations for ITSA" should {

    behave like checkRemoveAuthorisationForService("ITSA", deleteActiveITSARelationship(validArn.value, mtdItId.value, 204))
    val req = FakeRequest()

    "return 500  an exception if the relationship is not found" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = "ITSA")))
      deleteActiveITSARelationship(validArn.value, mtdItId.value, 404)

      an[Exception] should be thrownBy await(controller
        .submitRemoveAuthorisation("ITSA", "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientAll(req, validNino.nino, mtdItId.value).withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "return an exception if relationship service is unavailable" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = "ITSA")))
      deleteActiveITSARelationship(validArn.value, mtdItId.value, 500)

      an[Exception] should be thrownBy await(controller
        .submitRemoveAuthorisation( "ITSA", "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientAll(req, validNino.nino, mtdItId.value).withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "throw InsufficientEnrolments when Enrolment for chosen service is not found for logged in user" in {
      an[InsufficientEnrolments] shouldBe thrownBy {
        await(controller.submitRemoveAuthorisation("ITSA", "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientNi(req, validNino.nino).withFormUrlEncodedBody("confirmResponse" -> "true")))
      }
    }
  }

  "authorisationRemoved" should {

    "show authorisation_removed page with required sessions" in {
      val req = FakeRequest().withSession("agencyName" -> cache.agencyName, "service" -> cache.service)
      val result = await(controller.authorisationRemoved(authorisedAsClientAll(req, validNino.nino, mtdItId.value)))

      status(result) shouldBe 200
      checkHtmlResultWithBodyText(result, "This Agency Name")
      checkHtmlResultWithBodyText(result, "Some service name")
    }

    "return exception if required session data not found" in {
      val req = FakeRequest().withSession("agencyName" -> cache.agencyName)
      an[Exception] should be thrownBy await(controller.authorisationRemoved(authorisedAsClientAll(req, validNino.nino, mtdItId.value)))
    }
  }

  def checkRemoveAuthorisationForService(serviceName: String, deleteRelationshipStub: => Unit) = {
    implicit val req = FakeRequest()

    "return 200, remove the relationship if the client confirm deletion" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceName)))
      deleteRelationshipStub

      val result = await(controller.submitRemoveAuthorisation(serviceName, "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientAll(req, validNino.nino, mtdItId.value).withFormUrlEncodedBody("confirmResponse" -> "true")))

      status(result) shouldBe 303
      sessionStoreService.currentSession.clientCache.get.size == 0 shouldBe true

      result.session should not be empty
      result.session.get("agencyName") shouldBe Some(cache.agencyName)
      result.session.get("service") shouldBe Some(serviceName)
    }

    "redirect to manage-your-tax-agents if the client does not confirm deletion" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceName)))
      deleteRelationshipStub

      val result = await(controller.submitRemoveAuthorisation(serviceName, "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientAll(req, validNino.nino, mtdItId.value).withFormUrlEncodedBody("confirmResponse" -> "false")))

      status(result) shouldBe 303
      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "show error message if the client does not select any choice from the confirm delete radio buttons" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceName)))
      deleteRelationshipStub

      val result = await(controller.submitRemoveAuthorisation(serviceName, "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientAll(req, validNino.nino, mtdItId.value).withFormUrlEncodedBody("confirmResponse" -> "")))

      status(result) shouldBe 200
      checkHtmlResultWithBodyText(result, "Select yes if you want to remove your authorisation.")
      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "return an exception if the session cache is not found" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value)
      deleteRelationshipStub

      an[Exception] should be thrownBy await(controller
        .submitRemoveAuthorisation(serviceName, "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientAll(req, validNino.nino, mtdItId.value).withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache shouldBe empty
    }

    "return an exception if an invalid id is submitted" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceName)))
      deleteRelationshipStub

      an[Exception] should be thrownBy await(controller
        .submitRemoveAuthorisation(serviceName, "INVALID_ID")(authorisedAsClientAll(req, validNino.nino, mtdItId.value).withFormUrlEncodedBody("confirmResponse" -> "true")))
    }

    "remove deleted item from the session cache" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceName),
        cache.copy(uuId = "dc89f36b64c94060baa3ae87d6b7ac09next", service = serviceName)))
      sessionStoreService.currentSession.clientCache.get.size == 2 shouldBe true
      deleteRelationshipStub

      val result = await(controller.submitRemoveAuthorisation(serviceName, "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientAll(req, validNino.nino, mtdItId.value).withFormUrlEncodedBody("confirmResponse" -> "true")))

      status(result) shouldBe 303
      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
      sessionStoreService.currentSession.clientCache.get.head.uuId shouldBe "dc89f36b64c94060baa3ae87d6b7ac09next"
    }
  }
}
