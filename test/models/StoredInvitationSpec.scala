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

package models

import java.net.URL

import org.joda.time.{DateTime, LocalDate}
import play.utils.UriEncoding
import uk.gov.hmrc.agentclientmanagementfrontend
import uk.gov.hmrc.agentclientmanagementfrontend.models.{ClientCache, StoredInvitation}
import uk.gov.hmrc.agentclientmanagementfrontend.util.Services
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, Vrn}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.test.UnitSpec

class StoredInvitationSpec extends UnitSpec {

  val mtdItId = MtdItId("ABCDEF123456789")
  val validArn = Arn("FARN0001132")
  val validNino =  Nino("AE123456A")
  val validVrn =  Vrn("101747641")
  val created = DateTime.parse("2017-01-15T13:14:00.000+08:00")
  val lastUpdated = DateTime.parse("2018-01-15T13:14:00.000+08:00")
  val expiry = LocalDate.parse("2018-01-15")
  val startDate = Some(LocalDate.parse("2017-06-06"))
  val startDateString = "2017-06-06"
  val encodedClientId = UriEncoding.encodePathSegment(mtdItId.value, "UTF-8")
  val cache = ClientCache("dc89f36b64c94060baa3ae87d6b7ac08", validArn, "This Agency Name", "Some service name", startDate)
  val cacheItsa = ClientCache("dc89f36b64c94060baa3ae87d6b7ac08", validArn, "This Agency Name", "HMRC-MTD-IT", startDate)
  val serviceItsa = Services.HMRCMTDIT
  val serviceVat = Services.HMRCMTDVAT
  val serviceIrv = Services.HMRCPIR


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
    val storedInvitation= StoredInvitation(validArn, "personal", serviceItsa, mtdItId.value, "Pending", created, lastUpdated, expiry, "ATDMZYN4YDLNW", new URL("http://localhost:9432/agent-client-authorisation/clients/MTDITID/ABCDEF123456789/invitations/received/AGGNRK99U99D3"))
    "use the types apply method to return the correct parameter" in {
       storedInvitation.invitationId shouldBe "ATDMZYN4YDLNW"
       storedInvitation.clientId shouldBe mtdItId.value
       storedInvitation.status shouldBe "Pending"
    }
  }

}
