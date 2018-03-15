package uk.gov.hmrc.agentclientmanagementfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.agentclientmanagementfrontend.support.WireMockSupport
import play.api.test.FakeRequest
import uk.gov.hmrc.http.SessionKeys

trait AuthStubs {
  me: WireMockSupport =>

  case class Enrolment(serviceName: String, identifierName: String, identifierValue: String)

  def authorisedAsValidAgent[A](request: FakeRequest[A], arn: String) = authenticated(request, Set(Enrolment("HMRC-AS-AGENT", "AgentReferenceNumber", arn)), isAgent = true)

  def authorisedAsClientMtdItId[A](request: FakeRequest[A], mtdItId: String): FakeRequest[A] = authenticated(request, Set(Enrolment("HMRC-MTD-IT", "MTDITID", mtdItId)), isAgent = false )
  def authorisedAsClientNi[A](request: FakeRequest[A], nino: String): FakeRequest[A] = authenticated(request, Set(Enrolment("HMRC-NI", "NI", nino)), isAgent = false )
  def authorisedAsClientAll[A](request: FakeRequest[A], nino: String, mtdItId: String): FakeRequest[A] = authenticated(request, Set(Enrolment("HMRC-NI", "NI", nino), Enrolment("HMRC-MTD-IT", "MTDITID", mtdItId)), isAgent = false )

  def authenticated[A](request: FakeRequest[A], enrolment: Set[Enrolment], isAgent: Boolean): FakeRequest[A] = {
    val enrolmentJson = enrolment.map { x =>
    s"""
       |{ "key":"${x.serviceName}", "identifiers": [
       |      {"key":"${x.identifierName}", "value": "${x.identifierValue}"}
       |      ]}
     """.stripMargin
    }.mkString(",")

    givenAuthorisedFor(
      s"""
         |{
         |  "authorise": [
         |    {
         |            "$$or": [
         |                {
         |                    "enrolment": "HMRC-MTD-IT",
         |                    "identifiers": [],
         |                    "state": "Activated"
         |                },
         |                {
         |                    "enrolment": "HMRC-NI",
         |                    "identifiers": [],
         |                    "state": "Activated"
         |                }
         |            ]
         |        },
         |        {
         |            "authProviders": [
         |                "GovernmentGateway"
         |            ]
         |        }
         |  ],
         |  "retrieve":["authorisedEnrolments"]
         |}
           """.stripMargin,
      s"""
         |{
         |"authorisedEnrolments": [
         |  $enrolmentJson
         |]}
          """.stripMargin)
    request.withSession(request.session + SessionKeys.authToken -> "Bearer XYZ")
  }

  def givenUnauthorisedWith(mdtpDetail: String): Unit = {
    stubFor(post(urlEqualTo("/auth/authorise"))
      .willReturn(aResponse()
        .withStatus(401)
        .withHeader("WWW-Authenticate", s"""MDTP detail="$mdtpDetail"""")))
  }

  def givenAuthorisedFor(payload: String, responseBody: String): Unit = {
    stubFor(post(urlEqualTo("/auth/authorise"))
      .atPriority(1)
      .withRequestBody(equalToJson(payload, true, true))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody(responseBody)))

    stubFor(post(urlEqualTo("/auth/authorise")).atPriority(2)
      .willReturn(aResponse()
        .withStatus(401)
        .withHeader("WWW-Authenticate", "MDTP detail=\"InsufficientEnrolments\"")))
  }

  def verifyAuthoriseAttempt(): Unit = {
    verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))
  }

}