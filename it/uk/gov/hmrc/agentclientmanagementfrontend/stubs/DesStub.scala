package uk.gov.hmrc.agentclientmanagementfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.agentclientmanagementfrontend.support.WireMockSupport
import uk.gov.hmrc.agentmtdidentifiers.model.MtdItId
import uk.gov.hmrc.domain.Nino

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

  def givenNinoIsKnownFor(mtdbsa: MtdItId, nino: Nino) = {
    stubFor(
      get(urlEqualTo(s"/registration/business-details/mtdbsa/${mtdbsa.value}"))
        .willReturn(aResponse().withStatus(200).withBody(s"""{ "nino": "${nino.value}" }"""))
    )
  }

  def givenNinoIsUnknownFor(mtdbsa: MtdItId) = {
    stubFor(
      get(urlEqualTo(s"/registration/business-details/mtdbsa/${mtdbsa.value}"))
        .willReturn(aResponse().withStatus(404))
    )
  }

  def givenMtdbsaIsInvalid(mtdbsa: MtdItId) = {
    stubFor(
      get(urlMatching(s"/registration/.*?/mtdbsa/${mtdbsa.value}"))
        .willReturn(aResponse().withStatus(400))
    )
  }

  def givenDesReturnsServerError() = {
    stubFor(
      get(urlMatching(s"/registration/.*"))
        .willReturn(aResponse().withStatus(500))
    )
  }

  def givenDesReturnsServiceUnavailable() = {
    stubFor(
      get(urlMatching(s"/registration/.*"))
        .willReturn(aResponse().withStatus(503))
    )
  }
}