package uk.gov.hmrc.agentclientmanagementfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.agentclientmanagementfrontend.support.WireMockSupport

trait AgentClientRelationshipsStub {
  me: WireMockSupport =>

  def deleteActiveITSARelationship(arn: String, clientId: String, httpStatus: Int = 204): Unit = {
    stubFor(delete(urlEqualTo(s"/agent-client-relationships/agent/$arn/service/HMRC-MTD-IT/client/MTDITID/$clientId"))
      .willReturn(
        aResponse()
          .withStatus(httpStatus)))
  }

  def getClientActiveAgentRelationships(agentArn: String): Unit = {
    stubFor(get(urlEqualTo(s"/agent-client-relationships/service/HMRC-MTD-IT/client/relationship"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
               |{
               | "arn": "$agentArn"
               |}""".stripMargin)))
  }

  def get400ClientActiveAgentRelationships(): Unit = {
    stubFor(get(urlEqualTo(s"/agent-client-relationships/service/HMRC-MTD-IT/client/relationship"))
      .willReturn(
        aResponse()
          .withStatus(400)))
  }

  def getNotFoundClientActiveAgentRelationships(): Unit = {
    stubFor(get(urlEqualTo(s"/agent-client-relationships/service/HMRC-MTD-IT/client/relationship"))
      .willReturn(
        aResponse()
          .withStatus(404)))
  }

  def get500ClientActiveAgentRelationships(): Unit = {
    stubFor(get(urlEqualTo(s"/agent-client-relationships/service/HMRC-MTD-IT/client/relationship"))
      .willReturn(
        aResponse()
          .withStatus(500)))
  }

  def get503ClientActiveAgentRelationships(): Unit = {
    stubFor(get(urlEqualTo(s"/agent-client-relationships/service/HMRC-MTD-IT/client/relationship"))
      .willReturn(
        aResponse()
          .withStatus(503)))
  }

}
