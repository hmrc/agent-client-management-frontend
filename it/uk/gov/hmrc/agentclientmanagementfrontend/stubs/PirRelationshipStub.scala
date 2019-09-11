package uk.gov.hmrc.agentclientmanagementfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.agentclientmanagementfrontend.support.WireMockSupport
import uk.gov.hmrc.agentmtdidentifiers.model.Arn

trait PirRelationshipStub {
  me: WireMockSupport =>

  def getActivePIRRelationship(arn: Arn, service: String, nino: String, fromCesa: Boolean): StubMapping = {
    stubFor(get(urlEqualTo(s"/agent-fi-relationship/relationships/service/$service/clientId/$nino"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
               |[{
               |  "arn" : "${arn.value}",
               |  "service" : "$service",
               |  "clientId" : "$nino",
               |  "relationshipStatus" : "ACTIVE",
               |  "startDate" : "2017-12-08T15:21:51.040",
               |  "fromCesa" : $fromCesa
               |}]""".stripMargin)))
  }

  def getNotFoundForPIRRelationship(service: String, nino: String): StubMapping = {
    stubFor(get(urlEqualTo(s"/agent-fi-relationship/relationships/service/$service/clientId/$nino"))
      .willReturn(
        aResponse()
          .withStatus(404)))
  }

  def get500ForPIRRelationship(service: String, nino: String): StubMapping = {
    stubFor(get(urlEqualTo(s"/agent-fi-relationship/relationships/service/$service/clientId/$nino"))
      .willReturn(
        aResponse()
          .withStatus(500)))
  }

  def get503ForPIRRelationship(service: String, nino: String): StubMapping = {
    stubFor(get(urlEqualTo(s"/agent-fi-relationship/relationships/service/$service/clientId/$nino"))
      .willReturn(
        aResponse()
          .withStatus(503)))
  }

  def deleteActivePIRRelationship(arn: String, nino: String, httpStatus: Int = 200): StubMapping = {
    stubFor(delete(urlEqualTo(s"/agent-fi-relationship/relationships/agent/$arn/service/PERSONAL-INCOME-RECORD/client/$nino"))
      .willReturn(
        aResponse()
          .withStatus(httpStatus)))
  }
}
