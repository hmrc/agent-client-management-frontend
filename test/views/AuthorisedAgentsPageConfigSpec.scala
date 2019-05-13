/*
 * Copyright 2019 HM Revenue & Customs
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

import org.joda.time.{DateTime, LocalDate}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.Messages
import play.i18n.MessagesApi
import uk.gov.hmrc.agentclientmanagementfrontend.models.{AgentRequest, AuthorisedAgent}
import uk.gov.hmrc.agentclientmanagementfrontend.views.AuthorisedAgentsPageConfig
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.play.test.UnitSpec

class AuthorisedAgentsPageConfigSpec extends UnitSpec with OneAppPerSuite with MockitoSugar {

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val messages: Messages = mock[Messages]
  implicit def dateOrdering: Ordering[LocalDate] = Ordering.fromLessThan(_ isAfter _)


  val authAgent1 = AuthorisedAgent("uid123", "HMRC-MTD-IT", "Original Origami Org", Some(LocalDate.parse("2019-01-01")))

  val authAgents = Seq(authAgent1)

  val agentReq1 = AgentRequest("personal", "HMRC-MTD-IT", Arn("TARN0000001"), "uid123", "Parmesan Party Partnership", "Pending", LocalDate.now().plusDays(5), DateTime.now(), "invitationId")
  val agentReq2 = AgentRequest("personal", "HMRC-MTD-VAT", Arn("YARN3381592"), "uid123", "Cockeral Commander Co-op", "Pending", LocalDate.now().plusDays(5), DateTime.now(), "invitationId")
  val agentReq3 = AgentRequest("personal", "PERSONAL-INCOME-RECORD", Arn("TARN0000001"), "uid123", "Lightening Lifeboats Ltd", "Pending", LocalDate.now().plusDays(3), DateTime.now(), "invitationId")
  val agentReq4 = AgentRequest("personal", "PERSONAL-INCOME-RECORD", Arn("TARN0000001"), "uid123", "Coronation Cornet Corp", "Cancelled", LocalDate.now().plusDays(3), DateTime.now(), "invitationId")

  val agentReqs = Seq(agentReq1, agentReq2, agentReq3, agentReq4)

  val config: AuthorisedAgentsPageConfig = AuthorisedAgentsPageConfig(authAgents, agentReqs)(messages, dateOrdering)

  "AuthorisedAgentsPageConfig" should {
    "return pending requests which are agent specific and in expiry date order" in {
     config.pendingRequests shouldBe Seq(agentReq3, agentReq2)
    }

    "return number of non pending requests" in {
      config.nonPendingRequests shouldBe Seq(agentReq4)
    }

    "return true if there are pending requests" in {
      config.pendingRequestsExist shouldBe true
    }

    "return true if there are non pending requests" in {
      config.nonPendingRequestsExist shouldBe true
    }

    "return true if authorisations exist" in {
      config.authorisedAgentsExist shouldBe true
    }
  }

}
