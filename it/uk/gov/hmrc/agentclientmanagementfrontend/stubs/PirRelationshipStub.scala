package uk.gov.hmrc.agentclientmanagementfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlEqualTo}
import uk.gov.hmrc.agentclientmanagementfrontend.support.WireMockSupport
import uk.gov.hmrc.agentclientmanagementfrontend.util.Services
import uk.gov.hmrc.agentmtdidentifiers.model.Arn

trait PirRelationshipStub {
  me: WireMockSupport =>

  def getActiveAfiRelationship(arn: Arn, service: String, clientId: String, fromCesa: Boolean): Unit = {
    stubFor(get(urlEqualTo(s"/agent-fi-relationship/relationships/service/$service/clientId/$clientId"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
               |[{
               |  "arn" : "${arn.value}",
               |  "service" : "$service",
               |  "clientId" : "$clientId",
               |  "relationshipStatus" : "ACTIVE",
               |  "startDate" : "2017-12-08T15:21:51.040",
               |  "fromCesa" : $fromCesa
               |}]""".stripMargin)))
  }

  def getNotFoundForAfiRelationship(service: String, clientId: String): Unit = {
    stubFor(get(urlEqualTo(s"/agent-fi-relationship/relationships/service/$Services/clientId/$clientId"))
      .willReturn(
        aResponse()
          .withStatus(404)))
  }
}
