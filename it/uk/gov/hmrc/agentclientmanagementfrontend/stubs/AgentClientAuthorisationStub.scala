package uk.gov.hmrc.agentclientmanagementfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.agentmtdidentifiers.model.SuspensionDetails
import uk.gov.hmrc.agentclientmanagementfrontend.support.WireMockSupport
import uk.gov.hmrc.agentmtdidentifiers.model.Arn

trait AgentClientAuthorisationStub {
  me: WireMockSupport =>

  def getInvitations(arn: Arn, clientId: String, clientIdType:String, service: String, status: String, expiryDate: String, lastUpdated: String, clientType: String = "", isRelationshipEnded: Boolean = false, relationshipEndedBy: Option[String] = None) = {
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
               |   "isRelationshipEnded" : $isRelationshipEnded,
               |    ${relationshipEndedBy.map(v => s""" "relationshipEndedBy" : "$v", """).getOrElse("")}
               |   "status": "$status",
               |   "created": "2018-01-15T13:14:00.000+08:00",
               |   "lastUpdated": "$lastUpdated",
               |   "expiryDate": "$expiryDate",
               |   "invitationId": "ATDMZYN4YDLNW"
               |   }
               | ]
               |}}""".stripMargin)))
  }

  def getVatInvitations(arns: Seq[Arn], clientId: String) = {
    val invitations = arns.map{arn =>
      s"""
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
        |   "service": "HMRC-MTD-VAT",
        |   "clientId": "$clientId",
        |   "clientIdType": "VRN",
        |   "suppliedClientId": "$clientId",
        |   "suppliedClientIdType": "VRN",
        |   "isRelationshipEnded" : false,
        |   "relationshipEndedBy" : "",
        |   "status": "Rejected",
        |   "created": "2018-01-15T13:14:00.000+08:00",
        |   "lastUpdated": "2017-01-15T13:16:00.000+08:00",
        |   "expiryDate": "9999-01-01",
        |   "invitationId": "ATDMZYN4YDLNW"
        |   }
        |""".stripMargin
    }.mkString(",\r\n")
    stubFor(get(urlEqualTo(s"/agent-client-authorisation/clients/VRN/$clientId/invitations/received"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(s"""{ "_embedded": { "invitations": [ $invitations ] }}""" )))
  }


  def getInvitationsNotFound(clientId: String, clientIdType: String): StubMapping = {
    stubFor(get(urlEqualTo(s"/agent-client-authorisation/clients/$clientIdType/$clientId/invitations/received"))
      .willReturn(
        aResponse()
          .withStatus(404)))
  }

  def getAltItsaActiveRelationshipsNotFound(clientId: String): StubMapping = {
    stubFor(get(urlEqualTo(s"/agent-client-authorisation/clients/MTDITID/$clientId/invitations/received"))
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

  def getNAgencyNamesMap200(arnsWithNames: Map[Arn, String]): StubMapping = {
    val requestJson = arnsWithNames.keys.map(_.value).mkString("[\"","\",\"","\"]")
    val responseJson = arnsWithNames.toList.map(e => s"""{"arn": "${e._1.value}", "agencyName": "${e._2}" }""").mkString("[",",","]")
    stubFor(post(urlEqualTo(s"/agent-client-authorisation/client/agency-names"))
      .withHeader("Content-Type", containing("application/json"))
      .withRequestBody(equalToJson(requestJson, true, true))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(responseJson)
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

  def givenSetRelationshipEndedReturns(arn: Arn, clientId: String, status: Int) =
    stubFor(
      put(
        urlEqualTo(s"/agent-client-authorisation/invitations/set-relationship-ended"))
        .withRequestBody(
          equalToJson(
            s"""{
               |"arn": "${arn.value}",
               |"clientId": "$clientId",
               |"service": "HMRC-MTD-IT",
               |"endedBy": "Client"
               |}""".stripMargin))
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )
}
