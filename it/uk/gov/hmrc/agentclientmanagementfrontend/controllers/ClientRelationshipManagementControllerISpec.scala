package uk.gov.hmrc.agentclientmanagementfrontend.controllers

import play.api.test.FakeRequest
import play.utils.UriEncoding
import uk.gov.hmrc.agentclientmanagementfrontend.stubs.{AgentServicesAccountStub, AuthStubs, DesStub, PirRelationshipStub}
import uk.gov.hmrc.agentclientmanagementfrontend.support.BaseISpec
import uk.gov.hmrc.agentclientmanagementfrontend.util.Services
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}
import play.api.libs.ws.{WSClient, WSResponse}

class ClientRelationshipManagementControllerISpec extends BaseISpec with DesStub with PirRelationshipStub with AgentServicesAccountStub {

  private lazy val controller: ClientRelationshipManagementController = app.injector.instanceOf[ClientRelationshipManagementController]
  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  val urlJustWithPrefix = s"http://localhost:$wireMockPort/agent-client-management"
  val getViewAuthorisedAgentsRequest = (endOfUrl: String) => wsClient.url(s"$urlJustWithPrefix$endOfUrl").get()

  val mtdItId = MtdItId("AA123456A")
  val validArn = Arn("FARN0001132")

  "manageTaxAgents" should {
    val req = FakeRequest()
    val encodedClientId = UriEncoding.encodePathSegment(mtdItId.value, "UTF-8")
    "200, project authorised agents for a valid authenticated client with just PIR relationships " in {
      authorisedAsClient(req, mtdItId.value)
      getNotFoundClientActiveAgentRelationships(encodedClientId, Services.ITSA)
      getActiveAfiRelationship(validArn, Services.HMRCPIR, mtdItId.value, fromCesa = false)
      getAgencyNameMap200(validArn, "This Agency Name")

      val result = await(getViewAuthorisedAgentsRequest("/manage-your-tax-agents"))

      result.status shouldBe 200
    }

    "200, no authorised agents message for valid authenticated client with no relationships" in {

    }
  }
}
