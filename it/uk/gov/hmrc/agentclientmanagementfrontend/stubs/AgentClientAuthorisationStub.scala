package uk.gov.hmrc.agentclientmanagementfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.agentclientmanagementfrontend.models.SuspensionDetails
import uk.gov.hmrc.agentclientmanagementfrontend.support.WireMockSupport
import uk.gov.hmrc.agentmtdidentifiers.model.Arn

trait AgentClientAuthorisationStub {
  me: WireMockSupport =>

  def getInvitations(arn: Arn, clientId: String, clientIdType:String, service: String, status: String, expiryDate: String, lastUpdated: String, clientType: String = "") = {
    stubFor(get(urlEqualTo(s"/agent-client-authorisation/clients/$clientIdType/$clientId/invitations/received"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
               |{
               |"_embedded": {
               |"invitations": [
               |{
               |  "_links": {
               |    "accept": {
               |       "href": "/agent-client-authorisation/clients/MTDITID/ABCDEF123456789/invitations/received/A3A7PPJK1UBDY/accept"
               |     },
               |     "reject": {
               |       "href": "/agent-client-authorisation/clients/MTDITID/ABCDEF123456789/invitations/received/A3A7PPJK1UBDY/reject"
               |     },
               |     "self": {
               |        "href": "/agent-client-authorisation/clients/MTDITID/ABCDEF123456789/invitations/received/A3A7PPJK1UBDY"
               |     }
               |   },
               |   "arn": "${arn.value}",
               |   ${if(clientType.nonEmpty) s""""clientType": "$clientType","service": "$service",""" else s""""service": "$service","""}
               |   "clientId": "$clientId",
               |   "clientIdType": "$clientIdType",
               |   "suppliedClientId": "$clientId",
               |   "suppliedClientIdType": "$clientIdType",
               |   "status": "$status",
               |   "created": "2018-01-15T13:14:00.000+08:00",
               |   "lastUpdated": "$lastUpdated",
               |   "expiryDate": "$expiryDate",
               |   "invitationId": "ATDMZYN4YDLNW"
               |   }
               | ]
               |}}""".stripMargin)))
  }

  def getInvitationsNotFound(clientId: String, clientIdType: String): StubMapping = {
    stubFor(get(urlEqualTo(s"/agent-client-authorisation/clients/$clientIdType/$clientId/invitations/received"))
      .willReturn(
        aResponse()
          .withStatus(404)))
  }

  def givenAgentRefExistsFor(arn: Arn): StubMapping = {
    stubFor(get(urlEqualTo(s"/agencies/references/arn/${arn.value}"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""{
               |"arn": "${arn.value}",
               |"uid": "UID123"
               |}""".stripMargin

          )
      ))
  }

  def givenAgentRefNotFoundFor(arn: Arn): StubMapping = {
    stubFor(get(urlEqualTo(s"/agencies/references/arn/${arn.value}"))
      .willReturn(
        aResponse()
          .withStatus(404)
      ))
  }

  def getAgencyNameMap200(arn: Arn, agencyName: String): StubMapping = {
    stubFor(post(urlEqualTo(s"/agent-client-authorisation/client/agency-names"))
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
    stubFor(post(urlEqualTo(s"/agent-client-authorisation/client/agency-names"))
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
    stubFor(post(urlEqualTo(s"/agent-client-authorisation/client/agency-names"))
      .withHeader("Content-Type", containing("application/json"))
      .withRequestBody(equalTo(s"""["$invalidArn"]"""))
      .willReturn(
        aResponse()
          .withStatus(400)
      ))
  }

  def givenSuspensionDetails(arn: String, suspensionDetails: SuspensionDetails): StubMapping =
    stubFor(get(urlEqualTo(s"/agent-client-authorisation/client/suspension-details/$arn"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(Json.toJson(suspensionDetails).toString())
      ))
}
