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

package uk.gov.hmrc.agentclientmanagementfrontend.models

import java.time.LocalDateTime

import org.joda.time.LocalDate
import play.api.libs.json._
import uk.gov.hmrc.agentclientmanagementfrontend.util.Services
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import play.api.libs.functional.syntax._

sealed trait Relationship extends Product with Serializable {
  val arn: Arn
  val serviceName: String
  val dateFrom: Option[LocalDate]
}

case class ItsaRelationship(arn: Arn, dateFrom: Option[LocalDate]) extends Relationship {
  val serviceName = Services.HMRCMTDIT
}

object ItsaRelationship {
  implicit val relationshipWrites = Json.writes[ItsaRelationship]

  implicit val reads: Reads[ItsaRelationship] =
    ((JsPath \ "agentReferenceNumber").read[Arn] and
     (JsPath \ "dateFrom").readNullable[LocalDate])(ItsaRelationship.apply _)

}

case class PirRelationship(arn: Arn, dateFrom: Option[LocalDate]) extends Relationship {
  val serviceName = Services.HMRCPIR
}

object PirRelationship {
  implicit val relationshipWrites = Json.writes[PirRelationship]

  implicit val reads: Reads[PirRelationship] = (
    (JsPath \ "arn").read[Arn] and
    (JsPath \ "startDate").readNullable[LocalDateTime].map(date => javaDateTimeToJodaDate(date.get)))(PirRelationship.apply _)

  def javaDateTimeToJodaDate(javaTime: LocalDateTime): Option[LocalDate] = {
    LocalDate.parse(javaTime.toLocalDate.toString) match {
      case localDate => Some(localDate)
      case _ => None
    }
  }
}

case class VatRelationship(arn: Arn, dateFrom: Option[LocalDate]) extends Relationship {
  val serviceName = Services.HMRCMTDVAT
}

object VatRelationship {
  implicit val relationshipWrites = Json.writes[VatRelationship]

  implicit val reads: Reads[VatRelationship] =
    ((JsPath \ "agentReferenceNumber").read[Arn] and
     (JsPath \ "dateFrom").readNullable[LocalDate])(VatRelationship.apply _)

}
