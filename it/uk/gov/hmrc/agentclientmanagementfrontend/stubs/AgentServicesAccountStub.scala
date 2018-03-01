package uk.gov.hmrc.agentclientmanagementfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlEqualTo}
import uk.gov.hmrc.agentclientmanagementfrontend.support.WireMockSupport
import uk.gov.hmrc.agentmtdidentifiers.model.Arn

trait AgentServicesAccountStub {
  me: WireMockSupport =>

  def getAgencyNameMap200(arn: Arn, agencyName: String): Unit = {
    stubFor(post(urlEqualTo(s"/agent-services-account/client/agency-names"))
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

  def getTwoAgencyNamesMap200(): Unit = {
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
