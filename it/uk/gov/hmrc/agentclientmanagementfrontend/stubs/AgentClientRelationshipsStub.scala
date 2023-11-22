package uk.gov.hmrc.agentclientmanagementfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.agentclientmanagementfrontend.support.WireMockSupport
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentmtdidentifiers.model.Service.{HMRCCBCNONUKORG, HMRCCBCORG}

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

  def deleteActiveTrustNTRelationship(urn: String, clientId: String, httpStatus: Int = 204): StubMapping = {
    stubFor(delete(urlEqualTo(s"/agent-client-relationships/agent/$urn/service/HMRC-TERSNT-ORG/client/URN/$clientId"))
      .willReturn(
        aResponse()
          .withStatus(httpStatus)))
  }

  def deleteActivePptRelationship(arn: String, clientId: String, httpStatus: Int = 204): StubMapping = {
    stubFor(delete(urlEqualTo(s"/agent-client-relationships/agent/$arn/service/HMRC-PPT-ORG/client/EtmpRegistrationNumber/$clientId"))
      .willReturn(
        aResponse()
          .withStatus(httpStatus)))
  }

  def deleteActiveCbcRelationship(arn: String, clientId: String, isUKUser: Boolean, httpStatus: Int = 204): StubMapping = {
    val service = if(isUKUser) HMRCCBCORG else HMRCCBCNONUKORG
    stubFor(delete(urlEqualTo(s"/agent-client-relationships/agent/$arn/service/$service/client/cbcId/$clientId"))
      .willReturn(
        aResponse()
          .withStatus(httpStatus)))
  }

  def deleteActivePlrRelationship(arn: String, clientId: String, httpStatus: Int = 204): StubMapping = {
    stubFor(delete(urlEqualTo(s"/agent-client-relationships/agent/$arn/service/HMRC-PILLAR2-ORG/client/PLRID/$clientId"))
      .willReturn(
        aResponse()
          .withStatus(httpStatus)))
  }



  def getClientActiveAgentRelationships(service: String, agentArn: String, startDate: String): StubMapping = {
    stubFor(get(urlEqualTo(s"/agent-client-relationships/client/relationships/service/$service"))
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
    stubFor(get(urlEqualTo(s"/agent-client-relationships/client/relationships/service/$service"))
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
    stubFor(get(urlEqualTo(s"/agent-client-relationships/client/relationships/service/$service"))
      .willReturn(
        aResponse()
          .withStatus(400)))
  }

  def getNotFoundClientActiveAgentRelationships(service: String): StubMapping = {
    stubFor(get(urlEqualTo(s"/agent-client-relationships/client/relationships/service/$service"))
      .willReturn(
        aResponse()
          .withStatus(404)))
  }

  def get500ClientActiveAgentRelationships(service: String): StubMapping = {
    stubFor(get(urlEqualTo(s"/agent-client-relationships/client/relationships/service/$service"))
      .willReturn(
        aResponse()
          .withStatus(500)))
  }

  def get503ClientActiveAgentRelationships(service: String): StubMapping = {
    stubFor(get(urlEqualTo(s"/agent-client-relationships/client/relationships/service/$service"))
      .willReturn(
        aResponse()
          .withStatus(503)))
  }

  def getInactiveClientRelationshipsEmpty(): StubMapping = {
    stubFor(get(urlEqualTo(s"/agent-client-relationships/client/relationships/inactive"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""[]"""
          ))
    )
  }

  def getInactiveClientRelationshipsExist(arnVat: Arn, mtdItIdArn: Arn): StubMapping = {
    stubFor(get(urlEqualTo(s"/agent-client-relationships/client/relationships/inactive"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""[
               |{
               |"arn": "${arnVat.value}",
               |"service": "HMRC-MTD-VAT",
               |"dateFrom": "2017-01-05",
               |"dateTo": "2018-01-01"
               |},
               |{
               |"arn": "${mtdItIdArn.value}",
               |"service": "HMRC-MTD-IT",
               |"dateFrom": "2017-01-10",
               |"dateTo": "2018-10-10"
               |}]""".stripMargin
          )
      )
    )
  }
}
