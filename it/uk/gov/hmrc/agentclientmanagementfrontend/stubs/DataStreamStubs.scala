package uk.gov.hmrc.agentclientmanagementfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.libs.json.Json
import uk.gov.hmrc.agentclientmanagementfrontend.audit.AgentClientManagementEvent.AgentClientManagementEvent
import uk.gov.hmrc.agentclientmanagementfrontend.support.WireMockSupport

trait DataStreamStubs extends Eventually {
  me: WireMockSupport =>

  override implicit val patienceConfig = PatienceConfig(scaled(Span(5, Seconds)), scaled(Span(500, Millis)))

  def verifyAuditRequestSent(count: Int, event: AgentClientManagementEvent,
                             tags: Map[String, String] = Map.empty,
                             detail: Map[String, String] = Map.empty): Unit = {
    eventually {
      verify(1, postRequestedFor(urlPathEqualTo(auditUrl))
        .withRequestBody(similarToJson(
          s"""{
              |  "auditSource": "agent-client-management-frontend",
              |  "auditType": "$event",
              |  "tags": ${Json.toJson(tags)},
              |  "detail": ${Json.toJson(detail)}
              |}"""
        )))
    }
  }

  def verifyAuditRequestNotSent(event: AgentClientManagementEvent): Unit = {
    eventually {
      verify(0, postRequestedFor(urlPathEqualTo(auditUrl))
        .withRequestBody(similarToJson(
          s"""{
              |  "auditSource": "agent-invitations-frontend",
              |  "auditType": "$event"
              |}"""
        )))
    }
  }

  def givenAuditConnector(): Unit = {
    stubFor(post(urlPathEqualTo(auditUrl+"/merged")).willReturn(aResponse().withStatus(200)))
    stubFor(post(urlPathEqualTo(auditUrl)).willReturn(aResponse().withStatus(200)))
  }

  private def auditUrl = "/write/audit"

  private def similarToJson(value: String) = equalToJson(value.stripMargin, true, true)

}
