package uk.gov.hmrc.agentclientmanagementfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlEqualTo}
import uk.gov.hmrc.agentclientmanagementfrontend.support.WireMockSupport
import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn

trait AgentServicesAccountStub {
  me: WireMockSupport =>

  def getAgencyNameMap200(arn: Arn, agencyName: String): Unit = {
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

  def getTwoAgencyNamesMap200(arnWithName1: (Arn, String), arnWithName2: (Arn, String)): Unit = {
    stubFor(post(urlEqualTo(s"/agent-services-account/client/agency-names"))
      .withHeader("Content-Type", containing("application/json"))
      .withRequestBody(equalTo(s"""["${arnWithName1._1.value}","${arnWithName2._1.value}"]"""))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
               |[
               |{"arn": "${arnWithName1._1.value}", "agencyName": "${arnWithName1._2}" },
               |{"arn": "${arnWithName2._1.value}", "agencyName": "${arnWithName2._2}" }
               |]
            """.stripMargin)
      ))
  }

  def getAgencyNamesMap400(invalidArn: String): Unit = {
    stubFor(post(urlEqualTo(s"/agent-services-account/client/agency-names"))
      .withHeader("Content-Type", containing("application/json"))
      .withRequestBody(equalTo(s"""["$invalidArn"]"""))
      .willReturn(
        aResponse()
          .withStatus(400)
      ))
  }
}
