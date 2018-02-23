package uk.gov.hmrc.agentclientmanagementfrontend.stubs

import org.scalatest.concurrent.Eventually
import org.scalatest.time.{ Millis, Seconds, Span }
import play.api.libs.json.Json
import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.agentclientmanagementfrontend.services.AgentclientmanagementFrontendEvent.AgentclientmanagementFrontendEvent
import uk.gov.hmrc.agentclientmanagementfrontend.support.WireMockSupport

trait DataStreamStubs extends Eventually {
  me: WireMockSupport =>

  override implicit val patienceConfig = PatienceConfig(scaled(Span(5, Seconds)), scaled(Span(500, Millis)))

  def verifyAuditRequestSent(count: Int, event: AgentclientmanagementFrontendEvent, tags: Map[String, String] = Map.empty, detail: Map[String, String] = Map.empty): Unit = {
    eventually {
      verify(count, postRequestedFor(urlPathEqualTo(auditUrl))
        .withRequestBody(similarToJson(
          s"""{
              |  "auditSource": "agent-client-management-frontend",
              |  "auditType": "$event",
              |  "tags": ${Json.toJson(tags)},
              |  "detail": ${Json.toJson(detail)}
              |}""")))
    }
  }

  def verifyAuditRequestNotSent(event: AgentclientmanagementFrontendEvent): Unit = {
    eventually {
      verify(0, postRequestedFor(urlPathEqualTo(auditUrl))
        .withRequestBody(similarToJson(
          s"""{
              |  "auditSource": "agent-client-management-frontend",
              |  "auditType": "$event"
              |}""")))
    }
  }

  def givenAuditConnector(): Unit = {
    stubFor(post(urlPathEqualTo(auditUrl)).willReturn(aResponse().withStatus(204)))
    stubFor(post(urlPathEqualTo(auditUrl + "/merged")).willReturn(aResponse().withStatus(204)))
  }

  private def auditUrl = "/write/audit"

  private def similarToJson(value: String) = equalToJson(value.stripMargin, true, true)

}
