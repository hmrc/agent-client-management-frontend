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

package models

import java.net.URL
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

import uk.gov.hmrc.agentclientmanagementfrontend.models.StoredInvitation
import uk.gov.hmrc.agentclientmanagementfrontend.util.Services
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}
import uk.gov.hmrc.play.test.UnitSpec

class StoredInvitationSpec extends UnitSpec {

  private val mtdItId = MtdItId("ABCDEF123456789")
  private val validArn = Arn("FARN0001132")
  private val created = LocalDateTime.parse("2017-01-15T13:14:00.000+08:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME)
  private val lastUpdated = LocalDateTime.parse("2018-01-15T13:14:00.000+08:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME)
  private val expiry = LocalDate.parse("2018-01-15")
  private val serviceItsa = Services.HMRCMTDIT

  "clientIdTypeByService" should {
    "return the correct clientId type for the service" in {
      StoredInvitation.clientIdTypeByService("HMRC-MTD-IT") shouldBe "ni"
      StoredInvitation.clientIdTypeByService("PERSONAL-INCOME-RECORD") shouldBe "ni"
      StoredInvitation.clientIdTypeByService("HMRC-MTD-VAT") shouldBe "vrn"
    }
    "throw an illegal argument exception if the service is not supported" in {
      an[IllegalArgumentException] shouldBe thrownBy {
        StoredInvitation.clientIdTypeByService("foo")
      }
    }
  }

  "StoredInvitation. apply" should {
    val storedInvitation= StoredInvitation(validArn, Some("personal"), serviceItsa, mtdItId.value, "Pending", created, lastUpdated, expiry, "ATDMZYN4YDLNW", false, None, new URL("http://localhost:9432/agent-client-authorisation/clients/MTDITID/ABCDEF123456789/invitations/received/AGGNRK99U99D3"))
    "use the types apply method to return the correct parameter" in {
       storedInvitation.invitationId shouldBe "ATDMZYN4YDLNW"
       storedInvitation.clientId shouldBe mtdItId.value
       storedInvitation.status shouldBe "Pending"
    }

    "use the types apply method to return the correct parameter with Client type None" in {
      val storedInvitationNoCT = storedInvitation.copy(clientType = None)
      storedInvitationNoCT.clientType.isEmpty shouldBe true
      storedInvitationNoCT.invitationId shouldBe "ATDMZYN4YDLNW"
      storedInvitationNoCT.clientId shouldBe mtdItId.value
      storedInvitationNoCT.status shouldBe "Pending"
    }
  }

}
