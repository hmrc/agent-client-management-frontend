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

  def getInactivePIRRelationships(arn: Arn): StubMapping = {
    stubFor(get(urlEqualTo(s"/agent-fi-relationship/relationships/inactive"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
               |[
               |{
               |"arn": "${arn.value}",
               |"service": "INCOME-RECORD-VIEWER",
               |"clientId": "AE123456A",
               |"relationshipStatus": "TERMINATED",
               |"startDate": "2017-01-05T11:45:41.023",
               |"endDate": "2018-01-05T11:45:41.023",
               |"fromCesa": false
               |}]""".stripMargin
          ))
      )
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
