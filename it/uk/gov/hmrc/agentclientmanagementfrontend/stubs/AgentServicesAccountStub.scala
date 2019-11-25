package uk.gov.hmrc.agentclientmanagementfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlEqualTo}
import uk.gov.hmrc.agentclientmanagementfrontend.support.WireMockSupport
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.domain.Nino

trait AgentServicesAccountStub {
  me: WireMockSupport =>

  def getAgencyNameMap200(arn: Arn, agencyName: String): StubMapping = {
    stubFor(post(urlEqualTo(s"/agent-services-account/client/agency-names"))
      .withHeader("Content-Type", containing("application/json"))
      .withRequestBody(equalTo(s"""["${arn.value}"]"""))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
               |[
               |{"arn": "${arn.value}", "agencyName": "$agencyName" }
               |]
            """.stripMargin)
      ))
  }

  def getThreeAgencyNamesMap200(arnWithName1: (Arn, String), arnWithName2: (Arn, String), arnWithName3: (Arn, String)): StubMapping = {
    stubFor(post(urlEqualTo(s"/agent-services-account/client/agency-names"))
      .withHeader("Content-Type", containing("application/json"))
      .withRequestBody(equalToJson(s"""["${arnWithName1._1.value}","${arnWithName2._1.value}","${arnWithName3._1.value}"]""", true, true))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
               |[
               |{"arn": "${arnWithName1._1.value}", "agencyName": "${arnWithName1._2}" },
               |{"arn": "${arnWithName2._1.value}", "agencyName": "${arnWithName2._2}" },
               |{"arn": "${arnWithName3._1.value}", "agencyName": "${arnWithName3._2}" }
               |]
            """.stripMargin)
      ))
  }

  def getAgencyNamesMap400(invalidArn: String): StubMapping = {
    stubFor(post(urlEqualTo(s"/agent-services-account/client/agency-names"))
      .withHeader("Content-Type", containing("application/json"))
      .withRequestBody(equalTo(s"""["$invalidArn"]"""))
      .willReturn(
        aResponse()
          .withStatus(400)
      ))
  }

  def givenNinoIsKnownFor(nino: Nino): StubMapping = {
    stubFor(
      get(urlEqualTo(s"/agent-services-account/client/nino"))
        .willReturn(aResponse().withStatus(200).withBody(s"""{ "nino": "${nino.value}" }"""))
    )
  }

  def givenNinoIsUnknownFor: StubMapping = {
    stubFor(
      get(urlEqualTo(s"/agent-services-account/client/nino"))
        .willReturn(aResponse().withStatus(404))
    )
  }

  def givenGetNinoReturnsServerError: StubMapping = {
    stubFor(
      get(urlMatching(s"/agent-services-account/client/nino"))
        .willReturn(aResponse().withStatus(500))
    )
  }

  def givenGetNinoReturnsServiceUnavailable: StubMapping = {
    stubFor(
      get(urlMatching(s"/agent-services-account/client/nino"))
        .willReturn(aResponse().withStatus(503))
    )
  }
}
