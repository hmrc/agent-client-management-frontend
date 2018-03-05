package uk.gov.hmrc.agentclientmanagementfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlEqualTo}
import uk.gov.hmrc.agentclientmanagementfrontend.support.WireMockSupport
import uk.gov.hmrc.agentmtdidentifiers.model.Arn

trait DesStub {
  me: WireMockSupport =>

  def getClientActiveAgentRelationships(encodedClientId: String, service: String, agentArn: String): Unit = {
    stubFor(get(urlEqualTo(s"/registration/relationship?ref-no=$encodedClientId&agent=false&active-only=true&regime=$service"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
               |{
               |"relationship" :[
               |{
               |  "referenceNumber" : "ABCDE1234567890",
               |  "agentReferenceNumber" : "$agentArn",
               |  "organisation" : {
               |    "organisationName": "someOrganisationName"
               |  },
               |  "dateFrom" : "2015-09-10",
               |  "dateTo" : "2015-09-11",
               |  "contractAccountCategory" : "01",
               |  "activity" : "09"
               |}
               |]
               |}""".stripMargin)))
  }

  def get400ClientActiveAgentRelationships(encodedClientId: String, service: String): Unit = {
    stubFor(get(urlEqualTo(s"/registration/relationship?ref-no=$encodedClientId&agent=false&active-only=true&regime=$service"))
      .willReturn(
        aResponse()
          .withStatus(400)))
  }

  def getNotFoundClientActiveAgentRelationships(encodedClientId: String, service: String): Unit = {
    stubFor(get(urlEqualTo(s"/registration/relationship?ref-no=$encodedClientId&agent=false&active-only=true&regime=$service"))
      .willReturn(
        aResponse()
          .withStatus(404)))
  }

  def get500ClientActiveAgentRelationships(encodedClientId: String, service: String): Unit = {
    stubFor(get(urlEqualTo(s"/registration/relationship?ref-no=$encodedClientId&agent=false&active-only=true&regime=$service"))
      .willReturn(
        aResponse()
          .withStatus(500)))
  }

  def get503ClientActiveAgentRelationships(encodedClientId: String, service: String): Unit = {
    stubFor(get(urlEqualTo(s"/registration/relationship?ref-no=$encodedClientId&agent=false&active-only=true&regime=$service"))
      .willReturn(
        aResponse()
          .withStatus(503)))
  }
}