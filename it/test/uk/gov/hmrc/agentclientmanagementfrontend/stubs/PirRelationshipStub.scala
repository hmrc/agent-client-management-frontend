/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentclientmanagementfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.agentclientmanagementfrontend.support.WireMockSupport
import uk.gov.hmrc.agentmtdidentifiers.model.Arn

trait PirRelationshipStub {
  me: WireMockSupport =>

  def getActivePIRRelationship(arn: Arn, service: String, nino: String, fromCesa: Boolean): StubMapping =
    stubFor(
      get(urlEqualTo(s"/agent-fi-relationship/relationships/service/$service/clientId/$nino"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
                         |[{
                         |  "arn" : "${arn.value}",
                         |  "service" : "$service",
                         |  "clientId" : "$nino",
                         |  "relationshipStatus" : "ACTIVE",
                         |  "startDate" : "2017-12-08T15:21:51.040",
                         |  "fromCesa" : $fromCesa
                         |}]""".stripMargin)
        )
    )

  def getInactivePIRRelationships(arn: Arn): StubMapping =
    stubFor(
      get(urlEqualTo(s"/agent-fi-relationship/relationships/inactive"))
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
            )
        )
    )

  def getInactivePIRRelationshipsEmpty(): StubMapping =
    stubFor(
      get(urlEqualTo(s"/agent-fi-relationship/relationships/inactive"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"[]")
        )
    )

  def getNotFoundForPIRRelationship(service: String, nino: String): StubMapping =
    stubFor(
      get(urlEqualTo(s"/agent-fi-relationship/relationships/service/$service/clientId/$nino"))
        .willReturn(
          aResponse()
            .withStatus(404)
        )
    )

  def get500ForPIRRelationship(service: String, nino: String): StubMapping =
    stubFor(
      get(urlEqualTo(s"/agent-fi-relationship/relationships/service/$service/clientId/$nino"))
        .willReturn(
          aResponse()
            .withStatus(500)
        )
    )

  def get503ForPIRRelationship(service: String, nino: String): StubMapping =
    stubFor(
      get(urlEqualTo(s"/agent-fi-relationship/relationships/service/$service/clientId/$nino"))
        .willReturn(
          aResponse()
            .withStatus(503)
        )
    )

  def deleteActivePIRRelationship(arn: String, nino: String, httpStatus: Int = 200): StubMapping =
    stubFor(
      delete(urlEqualTo(s"/agent-fi-relationship/relationships/agent/$arn/service/PERSONAL-INCOME-RECORD/client/$nino"))
        .willReturn(
          aResponse()
            .withStatus(httpStatus)
        )
    )

  def getLegacyActiveSaRelationshipExists(utr: String, httpStatus: Int = 200): StubMapping =
    stubFor(
      get(urlEqualTo(s"/agent-fi-relationship/relationships/active-legacy-sa/utr/$utr"))
        .willReturn(
          aResponse()
            .withStatus(httpStatus)
        )
    )
}
