/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentclientmanagementfrontend.models

import play.api.libs.json.{JsSuccess, JsValue, Json}
import uk.gov.hmrc.agentclientmanagementfrontend.support.UnitSpec
import uk.gov.hmrc.agentmtdidentifiers.model.Arn

import java.time.{LocalDate, LocalDateTime}

class RelationshipSpec extends UnitSpec {

  val ACRjson: JsValue = Json.parse("""
                                      |{
                                      |"agentReferenceNumber":"TARN0001",
                                      |"dateFrom":"2017-06-06"
                                      |}
    """.stripMargin)

  val AFIRjson: JsValue = Json.parse(
    """
      |{
      |"arn": "TARN0001",
      |"service": "PERSONAL-INCOME-RECORD",
      |"clientId": "AB123456A",
      |"relationshipStatus": "Active",
      |"startDate": "2017-06-06T11:27:51.45",
      |"endDate": "2017-08-08",
      |"fromCesa": "false"
      |}
    """.stripMargin
  )

  val APlrjson: JsValue = Json.parse(
    """
      |{
      |"agentReferenceNumber":"TARN0001",
      |"dateFrom":"2017-06-06"
      |}
    """.stripMargin
  )

  "ItsaRelationships reads method" should {
    "successfully read the json and convert it to ItsaRelationship type" in {
      ACRjson.validate[ItsaRelationship](ItsaRelationship.reads) shouldBe
        JsSuccess(ItsaRelationship(Arn("TARN0001"), Some(LocalDate.parse("2017-06-06"))))
    }
  }
  "VatRelationships reads method" should {
    "successfully read the json and convert it to VatRelationship type" in {
      ACRjson.validate[VatRelationship](VatRelationship.reads) shouldBe
        JsSuccess(VatRelationship(Arn("TARN0001"), Some(LocalDate.parse("2017-06-06"))))
    }
  }
  "PirRelationships reads method" should {
    "successfully read the json and convert it to PirRelationship type" in {
      AFIRjson.validate[PirRelationship](PirRelationship.reads) shouldBe
        JsSuccess(PirRelationship(Arn("TARN0001"), Some(LocalDate.parse("2017-06-06"))))
    }
  }

  "PlrRelationship reads method" should {
    "successfully read the json and convert it to PlrRelationship type" in {
      ACRjson.validate[PlrRelationship](PlrRelationship.reads) shouldBe
        JsSuccess(PlrRelationship(Arn("TARN0001"), Some(LocalDate.parse("2017-06-06"))))
    }
  }

  "javaDateTimeToJodaDate" should {
    "convert the time from java.LocalDateTime format into joda.LocalDate format" in {
      PirRelationship.javaDateTimeToJodaDate(LocalDateTime.now) shouldBe Some(LocalDate.now)
    }
  }
}
