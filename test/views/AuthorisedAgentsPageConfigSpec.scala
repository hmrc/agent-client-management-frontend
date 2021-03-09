/*
 * Copyright 2021 HM Revenue & Customs
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

package views

import java.time.{LocalDate, LocalDateTime, ZoneOffset}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Configuration, Environment}
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.test.FakeRequest
import uk.gov.hmrc.agentclientmanagementfrontend.config.{AppConfig, FrontendAppConfig}
import uk.gov.hmrc.agentclientmanagementfrontend.models.{AgentRequest, AuthorisedAgent}
import uk.gov.hmrc.agentclientmanagementfrontend.views.AuthorisedAgentsPageConfig
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.test.UnitSpec


class AuthorisedAgentsPageConfigSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar {

  implicit def dateOrdering: Ordering[LocalDate] = Ordering.fromLessThan(_ isAfter _)

  implicit val request = FakeRequest()

  val messagesApi = app.injector.instanceOf[MessagesApi]
  implicit val messages = MessagesImpl(Lang("en"), messagesApi)

  val authAgent1 = AuthorisedAgent("uid123", "HMRC-MTD-IT", "Original Origami Org", Some(LocalDate.parse("2019-01-01")))

  val authAgents = Seq(authAgent1)

  val agentReq1 = AgentRequest("personal", "HMRC-MTD-IT", Arn("TARN0000001"), "uid123", "Parmesan Party Partnership", "Pending", LocalDate.now().plusDays(5), LocalDateTime.now(ZoneOffset.UTC), "invitationId")
  val agentReq2 = AgentRequest("personal", "HMRC-MTD-VAT", Arn("YARN3381592"), "uid123", "Cockeral Commander Co-op", "Pending", LocalDate.now().plusDays(5), LocalDateTime.now(ZoneOffset.UTC), "invitationId")
  val agentReq3 = AgentRequest("personal", "PERSONAL-INCOME-RECORD", Arn("TARN0000001"), "uid123", "Lightening Lifeboats Ltd", "Pending", LocalDate.now().plusDays(3), LocalDateTime.now(ZoneOffset.UTC), "invitationId")
  val agentReq4 = AgentRequest("personal", "PERSONAL-INCOME-RECORD", Arn("TARN0000001"), "uid123", "Coronation Cornet Corp", "Cancelled", LocalDate.now().plusDays(3), LocalDateTime.now(ZoneOffset.UTC), "invitationId")

  val agentReqs = Seq(agentReq1, agentReq2, agentReq3, agentReq4)

  val env = Environment.simple()
  val configuration = new ServicesConfig(Configuration.load(env))
  val appConfig = new FrontendAppConfig(configuration)

  val config: AuthorisedAgentsPageConfig = AuthorisedAgentsPageConfig(authAgents, agentReqs)(request ,dateOrdering, messages, appConfig )

  "AuthorisedAgentsPageConfig" should {
    "return pending requests which are agent specific and in expiry date order" in {
     config.displayValidPendingRequests shouldBe Seq(
       agentReq3.copy(serviceName = "2 tax services"),
       agentReq2.copy(serviceName = "Manage your VAT")
     )
    }

    "return number of non pending requests" in {
      config.validNonPendingRequests shouldBe Seq(agentReq4)
    }

    "return true if there are pending requests" in {
      config.validPendingRequestsExist shouldBe true
    }

    "return true if there are non pending requests" in {
      config.validNonPendingRequestsExist shouldBe true
    }

    "return true if authorisations exist" in {
      config.authorisedAgentsExist shouldBe true
    }

    "return number of pending requests" in {
      config.validPendingCount shouldBe 2
    }

    "prettify the date" in {
      val dateBigDay = Some(LocalDate.parse("1993-09-21"))
      val dateSmallDay = Some(LocalDate.parse("2019-04-01"))

      config.displayDate(dateBigDay) shouldBe "21 September 1993"
      config.displayDate(dateSmallDay) shouldBe "1 April 2019"
    }
  }

}
