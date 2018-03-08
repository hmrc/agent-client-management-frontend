package uk.gov.hmrc.agentclientmanagementfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, delete, stubFor, urlEqualTo}
import uk.gov.hmrc.agentclientmanagementfrontend.support.WireMockSupport

trait AgentClientRelationshipsStub {
  me: WireMockSupport =>

  def deleteActiveITSARelationship(arn: String, clientId: String, httpStatus: Int = 204): Unit = {
    stubFor(delete(urlEqualTo(s"/agent-client-relationships/agent/$arn/service/HMRC-MTD-IT/client/MTDITID/$clientId"))
      .willReturn(
        aResponse()
          .withStatus(httpStatus)))
  }

}
