/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.libs.json.{JsPath, JsSuccess, JsValue, Json}
import uk.gov.hmrc.agentclientmanagementfrontend.models.{ItsaRelationship, VatRelationship}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.play.test.UnitSpec


class RelationshipSpec extends UnitSpec {

  val json: JsValue = Json.parse(
    """
      |{
      |"agentReferenceNumber":"TARN0001"
      |}
    """.stripMargin)

  "ItsaRelationships reads method" should {
    "successfully read the json and convert it to ItsaRelationship type" in {
      json.validate[ItsaRelationship](ItsaRelationship.reads) shouldBe
        JsSuccess(ItsaRelationship(Arn("TARN0001")),JsPath \ "agentReferenceNumber")
    }
  }
  "VatRelationships reads method" should {
    "successfully read the json and convert it to VatRelationship type" in {
      json.validate[VatRelationship](VatRelationship.reads) shouldBe
        JsSuccess(VatRelationship(Arn("TARN0001")),JsPath \ "agentReferenceNumber")
    }
  }
}
