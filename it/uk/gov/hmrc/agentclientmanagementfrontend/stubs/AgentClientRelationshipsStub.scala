package uk.gov.hmrc.agentclientmanagementfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.{MatchResult, UrlPattern}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.agentclientmanagementfrontend.support.WireMockSupport

trait AgentClientRelationshipsStub {
  me: WireMockSupport =>

  def deleteActiveITSARelationship(arn: String, clientId: String, httpStatus: Int = 204): StubMapping = {
    stubFor(delete(urlEqualTo(s"/agent-client-relationships/agent/$arn/service/HMRC-MTD-IT/client/MTDITID/$clientId"))
      .willReturn(
        aResponse()
          .withStatus(httpStatus)))
  }

  def deleteActiveVATRelationship(arn: String, clientId: String, httpStatus: Int = 204): StubMapping = {
    stubFor(delete(urlEqualTo(s"/agent-client-relationships/agent/$arn/service/HMRC-MTD-VAT/client/VRN/$clientId"))
      .willReturn(
        aResponse()
          .withStatus(httpStatus)))
  }

  def deleteActiveTrustRelationship(arn: String, clientId: String, httpStatus: Int = 204): StubMapping = {
    stubFor(delete(urlEqualTo(s"/agent-client-relationships/agent/$arn/service/HMRC-TERS-ORG/client/SAUTR/$clientId"))
      .willReturn(
        aResponse()
          .withStatus(httpStatus)))
  }

  def getClientActiveAgentRelationships(service: String, agentArn: String, startDate: String): StubMapping = {
    stubFor(get(urlEqualTo(s"/agent-client-relationships/relationships/service/$service"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
               |{
               | "arn": "$agentArn",
               | "dateFrom": "$startDate"
               |}""".stripMargin)))
  }

  def getClientActiveAgentRelationshipsNoStartDate(service: String, agentArn: String): StubMapping = {
    stubFor(get(urlEqualTo(s"/agent-client-relationships/relationships/service/$service"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
               |{
               | "arn": "$agentArn"
               |}""".stripMargin)))
  }

  def get400ClientActiveAgentRelationships(service: String): StubMapping = {
    stubFor(get(urlEqualTo(s"/agent-client-relationships/relationships/service/$service"))
      .willReturn(
        aResponse()
          .withStatus(400)))
  }

  def getNotFoundClientActiveAgentRelationships(service: String): StubMapping = {
    stubFor(get(urlEqualTo(s"/agent-client-relationships/relationships/service/$service"))
      .willReturn(
        aResponse()
          .withStatus(404)))
  }

  def get500ClientActiveAgentRelationships(service: String): StubMapping = {
    stubFor(get(urlEqualTo(s"/agent-client-relationships/relationships/service/$service"))
      .willReturn(
        aResponse()
          .withStatus(500)))
  }

  def get503ClientActiveAgentRelationships(service: String): StubMapping = {
    stubFor(get(urlEqualTo(s"/agent-client-relationships/relationships/service/$service"))
      .willReturn(
        aResponse()
          .withStatus(503)))
  }
}
