package uk.gov.hmrc.agentclientmanagementfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlEqualTo}
import uk.gov.hmrc.agentclientmanagementfrontend.support.WireMockSupport

trait AgentServicesAccountStub {
  me: WireMockSupport =>

  def getAgencyNamesMap200(): Unit = {
    stubFor(post(urlEqualTo(s"/agent-services-account/client/agency-names"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
               |[
               |{"arn": "TARN0000010", "agencyName": "someName" },
               |{"arn": "TARN0000011", "agencyName": "someName1" }
               |]
            """.stripMargin)
      ))
  }

  def getAgencyNamesMap400(): Unit = {
    stubFor(post(urlEqualTo(s"/agent-services-account/client/agency-names"))
      .willReturn(
        aResponse()
          .withStatus(400)
      ))
  }
}
