/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.{LocalDate, LocalDateTime}

import play.api.libs.json.{JsPath, Json, Reads}
import uk.gov.hmrc.agentclientmanagementfrontend.util.Services
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import play.api.libs.functional.syntax._

sealed trait Inactive extends Product with Serializable {
  val arn: Arn
  val serviceName: String
  val dateFrom: Option[LocalDate]
  val dateTo: Option[LocalDate]
}

case class InactiveRelationship(arn: Arn, dateFrom: Option[LocalDate], dateTo: Option[LocalDate], serviceName: String) extends Inactive

object InactiveRelationship{

  implicit val inactiveRelationshipWrites = Json.writes[InactiveRelationship]

  implicit val reads: Reads[InactiveRelationship] = (
    (JsPath \ "arn").read[Arn] and
      (JsPath \ "dateFrom").readNullable[LocalDate] and
      (JsPath \ "dateTo").readNullable[LocalDate] and
      (JsPath \ "service").read[String])(InactiveRelationship.apply _)
}


case class PirInactiveRelationship(arn: Arn, dateFrom: Option[LocalDate], dateTo: Option[LocalDate]) extends Inactive {
  val serviceName = Services.HMRCPIR
}

object PirInactiveRelationship {
  implicit val inactiveRelationshipWrites = Json.writes[PirInactiveRelationship]

  implicit val reads: Reads[PirInactiveRelationship] = (
    (JsPath \ "arn").read[Arn] and
      (JsPath \ "startDate").readNullable[LocalDateTime].map(date => javaDateTimeToJodaDate(date.get)) and
      (JsPath \ "endDate").readNullable[LocalDateTime].map(date => javaDateTimeToJodaDate(date.get)))(PirInactiveRelationship.apply _)

  def javaDateTimeToJodaDate(javaTime: LocalDateTime): Option[LocalDate] = {
    Some(LocalDate.parse(javaTime.toLocalDate.toString))
  }
}