package uk.gov.hmrc.agentclientmanagementfrontend.controllers

import play.api.test.{FakeRequest, Helpers}
import play.utils.UriEncoding
import uk.gov.hmrc.agentclientmanagementfrontend.stubs.{AgentServicesAccountStub, AuthStubs, DesStub, PirRelationshipStub}
import uk.gov.hmrc.agentclientmanagementfrontend.support.BaseISpec
import uk.gov.hmrc.agentclientmanagementfrontend.util.Services
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}
import play.api.libs.ws.{WSClient, WSResponse}
import uk.gov.hmrc.agentclientmanagementfrontend.models.ArnCache
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId

class ClientRelationshipManagementControllerISpec extends BaseISpec with DesStub with PirRelationshipStub with AgentServicesAccountStub {

  private lazy val controller: ClientRelationshipManagementController = app.injector.instanceOf[ClientRelationshipManagementController]
  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  private implicit val hc = HeaderCarrier(sessionId = Some(SessionId("sessionId123456")))
  val urlJustWithPrefix = s"http://localhost:$port/manage-your-tax-agents"
  val getViewAuthorisedAgentsRequest = (endOfUrl: String) => wsClient.url(s"$urlJustWithPrefix$endOfUrl").get()

  val mtdItId = MtdItId("AA123456A")
  val validArn = Arn("FARN0001132")

  "manageTaxAgents" should {
    val req = FakeRequest()
    val encodedClientId = UriEncoding.encodePathSegment(mtdItId.value, "UTF-8")
    "200, project authorised agent for a valid authenticated client with just PIR relationship" in {
      authorisedAsClient(req, mtdItId.value)
      getNotFoundClientActiveAgentRelationships(encodedClientId, Services.ITSA)
      getActiveAfiRelationship(validArn, Services.HMRCPIR, mtdItId.value, fromCesa = false)
      getAgencyNameMap200(validArn, "This Agency Name")

      val result = await(getViewAuthorisedAgentsRequest(""))

      result.status shouldBe 200
      result.body.contains("This Agency Name") shouldBe true
      sessionStoreService.currentSession.arnCache.get.size == 1 shouldBe true
    }

    "200, project authorised agent for a valid authenticated client with just Itsa relationship" in {
      authorisedAsClient(req, mtdItId.value)
      getClientActiveAgentRelationships(encodedClientId,Services.ITSA, validArn.value)
      getNotFoundForAfiRelationship(Services.HMRCPIR, mtdItId.value)
      getAgencyNameMap200(validArn, "This Agency Name")

      val result = await(getViewAuthorisedAgentsRequest(""))

      result.status shouldBe 200
      result.body.contains("This Agency Name") shouldBe true
      sessionStoreService.currentSession.arnCache.get.size == 1 shouldBe true
    }

    "200, project authorised agents for valid authenticated client with ITSA and PIR relationship" in {
      authorisedAsClient(req, mtdItId.value)
      getClientActiveAgentRelationships(encodedClientId,Services.ITSA, validArn.value)
      getActiveAfiRelationship(validArn.copy(value="FARN0001131"), Services.HMRCPIR, mtdItId.value, fromCesa = false)
      getTwoAgencyNamesMap200((validArn,"This Agency Name"),(validArn.copy(value="FARN0001131"),"Different"))

      val result = await(getViewAuthorisedAgentsRequest(""))

      result.status shouldBe 200
      result.body.contains("This Agency Name") shouldBe true
      result.body.contains("Different") shouldBe true
      sessionStoreService.currentSession.arnCache.get.size == 2 shouldBe true
    }

    "200, no authorised agents message for valid authenticated client with no relationships" in {
      authorisedAsClient(req, mtdItId.value)
      getNotFoundClientActiveAgentRelationships(encodedClientId, Services.ITSA)
      getNotFoundForAfiRelationship(Services.HMRCPIR, mtdItId.value)

      val result = await(getViewAuthorisedAgentsRequest(""))

      result.status shouldBe 200
      result.body.contains("You have no authorised agents") shouldBe true
      sessionStoreService.currentSession.arnCache.get.isEmpty shouldBe true
    }

    "500, when getAgencyNames in agent-services-account returns 400 invalid Arn" in {
      authorisedAsClient(req, mtdItId.value)
      getClientActiveAgentRelationships(encodedClientId,Services.ITSA, Arn("someInvalidArn").value)
      getActiveAfiRelationship(validArn, Services.HMRCPIR, mtdItId.value, fromCesa = false)
      getAgencyNamesMap400("someInvalidArn")

      val result = await(getViewAuthorisedAgentsRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
      sessionStoreService.currentSession.arnCache.isDefined shouldBe false
    }

    "500, when getAgencyNames in agent-services-account returns 400 empty Arn" in {
      authorisedAsClient(req, mtdItId.value)
      getClientActiveAgentRelationships(encodedClientId,Services.ITSA, Arn("").value)
      getActiveAfiRelationship(validArn, Services.HMRCPIR, mtdItId.value, fromCesa = false)
      getAgencyNamesMap400("")

      val result = await(getViewAuthorisedAgentsRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
      sessionStoreService.currentSession.arnCache.isDefined shouldBe false
    }

    "500, when Des returns 400" in {
      authorisedAsClient(req, mtdItId.value)
      get400ClientActiveAgentRelationships(encodedClientId, Services.ITSA)
      getActiveAfiRelationship(validArn, Services.HMRCPIR, mtdItId.value, fromCesa = false)

      val result = await(getViewAuthorisedAgentsRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
      sessionStoreService.currentSession.arnCache.isDefined shouldBe false
    }

    "500, when Des returns 500" in {
      authorisedAsClient(req, mtdItId.value)
      get500ClientActiveAgentRelationships(encodedClientId, Services.ITSA)
      getNotFoundForAfiRelationship(Services.HMRCPIR, mtdItId.value)

      val result = await(getViewAuthorisedAgentsRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
      sessionStoreService.currentSession.arnCache.isDefined shouldBe false
    }

    "500, when Des returns 503" in {
      authorisedAsClient(req, mtdItId.value)
      get503ClientActiveAgentRelationships(encodedClientId, Services.ITSA)
      getActiveAfiRelationship(validArn, Services.HMRCPIR, mtdItId.value, fromCesa = false)

      val result = await(getViewAuthorisedAgentsRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
      sessionStoreService.currentSession.arnCache.isDefined shouldBe false
    }

    "500, when agent-fi-relationship returns 500" in {
      authorisedAsClient(req, mtdItId.value)
      getClientActiveAgentRelationships(encodedClientId,Services.ITSA, validArn.value)
      get500ForAfiRelationship(Services.HMRCPIR, mtdItId.value)

      val result = await(getViewAuthorisedAgentsRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
      sessionStoreService.currentSession.arnCache.isDefined shouldBe false
    }

    "500, when agent-fi-relationship returns 503" in {
      authorisedAsClient(req, mtdItId.value)
      getNotFoundClientActiveAgentRelationships(encodedClientId, Services.ITSA)
      get503ForAfiRelationship(Services.HMRCPIR, mtdItId.value)

      val result = await(getViewAuthorisedAgentsRequest(""))

      result.status shouldBe 500
      result.body.contains("Sorry, we’re experiencing technical difficulties") shouldBe true
      sessionStoreService.currentSession.arnCache.isDefined shouldBe false
    }
  }
}
