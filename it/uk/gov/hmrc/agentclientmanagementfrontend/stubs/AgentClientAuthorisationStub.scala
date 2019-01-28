package uk.gov.hmrc.agentclientmanagementfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.agentclientmanagementfrontend.support.WireMockSupport
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, Vrn}
import uk.gov.hmrc.domain.Nino

trait AgentClientAuthorisationStub {
  me: WireMockSupport =>

  def getInvitations(arn: Arn, clientId: String, clientIdType:String, service: String, status: String, expiryDate: String, lastUpdated: String) = {
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
               |   "clientType": "personal",
               |   "service": "$service",
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

  def getInvitationsNotFound(clientId: String, clientIdType: String): Unit = {
    stubFor(get(urlEqualTo(s"/agent-client-authorisation/clients/$clientIdType/$clientId/invitations/received"))
      .willReturn(
        aResponse()
          .withStatus(404)))
  }

  def givenAgentRefExistsFor(arn: Arn): Unit = {
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

  def givenAgentRefNotFoundFor(arn: Arn): Unit = {
    stubFor(get(urlEqualTo(s"/agencies/references/arn/${arn.value}"))
      .willReturn(
        aResponse()
          .withStatus(404)
      ))
  }

}
