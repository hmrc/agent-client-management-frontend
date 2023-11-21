package uk.gov.hmrc.agentclientmanagementfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.agentclientmanagementfrontend.support.WireMockSupport
import play.api.test.FakeRequest
import uk.gov.hmrc.http.SessionKeys

trait AuthStubs {
  me: WireMockSupport =>

  case class Enrolment(serviceName: String, identifierName: String, identifierValue: String)

  def authorisedAsClientMtdItId[A](request: FakeRequest[A], mtdItId: String): FakeRequest[A] = authenticated(request, Set(Enrolment("HMRC-MTD-IT", "MTDITID", mtdItId)), isAgent = false )
  def authorisedAsClientTrustNtUrn[A](request: FakeRequest[A], urn: String): FakeRequest[A] = authenticated(request, Set(Enrolment("HMRC-TERSNT-ORG", "URN", urn)), isAgent = false )
  def authorisedAsClientMtdItIdWithIrSa[A](request: FakeRequest[A], mtdItId: String, utr: String): FakeRequest[A] = authenticated(request, Set(Enrolment("HMRC-MTD-IT", "MTDITID", mtdItId), Enrolment("IR-SA", "UTR", utr)), isAgent = false)
  def authorisedAsClientNi[A](request: FakeRequest[A], nino: String): FakeRequest[A] = authenticated(request, Set(Enrolment("HMRC-NI", "NINO", nino)), isAgent = false )
  def authorisedAsClientVat[A](request: FakeRequest[A], vrn: String): FakeRequest[A] = authenticated(request, Set(Enrolment("HMRC-MTD-VAT", "VRN", vrn)), isAgent = false )
  def authorisedAsClientAll[A](request: FakeRequest[A], nino: String, mtdItId: String, vrn:String, utr: String, urn: String, cgtRef: String, pptRef: String, cbcUKRef: String, cbcNonUKRef: String, plrId: String): FakeRequest[A] =
    authenticated(request, Set(
      Enrolment("HMRC-NI", "NINO", nino),
      Enrolment("HMRC-MTD-IT", "MTDITID", mtdItId),
      Enrolment("HMRC-MTD-VAT", "VRN", vrn),
      Enrolment("HMRC-TERS-ORG", "SAUTR", utr),
      Enrolment("HMRC-CGT-PD", "CGTPDRef", cgtRef),
      Enrolment("HMRC-TERSNT-ORG", "URN", urn),
      Enrolment("HMRC-PPT-ORG", "EtmpRegistrationNumber", pptRef),
      Enrolment("HMRC-CBC-ORG", "cbcId", cbcUKRef),
      Enrolment("HMRC-CBC-NONUK-ORG", "cbcId", cbcNonUKRef),
      Enrolment("HMRC-PILLAR2-ORG", "PLRID", plrId)
    ), isAgent = false)

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
         |        "authProviders": [
         |           "GovernmentGateway"
         |        ]
         |    }
         |  ],
         |  "retrieve":["affinityGroup","allEnrolments"]
         |}
           """.stripMargin,
      s"""
         |{
         |"affinityGroup": "Individual",
         |"allEnrolments": [
         |  $enrolmentJson
         |]}
          """.stripMargin)
    request.withSession(request.session + SessionKeys.authToken -> "Bearer XYZ")
  }

  def givenUnauthorisedWith(mdtpDetail: String): StubMapping = {
    stubFor(post(urlEqualTo("/auth/authorise"))
      .willReturn(aResponse()
        .withStatus(401)
        .withHeader("WWW-Authenticate", s"""MDTP detail="$mdtpDetail"""")))
  }

  def givenAuthorisedFor(payload: String, responseBody: String): StubMapping = {
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