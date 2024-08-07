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

import java.time.LocalDateTime
import java.time.LocalDate
import play.api.libs.json._
import uk.gov.hmrc.agentclientmanagementfrontend.util.Services
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import play.api.libs.functional.syntax._

sealed trait Relationship extends Product with Serializable {
  val arn: Arn
  val serviceName: String
  val dateFrom: Option[LocalDate]
  val isAltItsa: Boolean
}

case class ItsaRelationship(arn: Arn, dateFrom: Option[LocalDate]) extends Relationship {
  val serviceName = Services.HMRCMTDIT
  val isAltItsa = false
}

case class AltItsaRelationship(arn: Arn, dateFrom: Option[LocalDate]) extends Relationship {
  val serviceName = Services.HMRCMTDIT
  val isAltItsa = true
}

object AltItsaRelationship {
  implicit val altItsaFormat: OFormat[AltItsaRelationship] = Json.format[AltItsaRelationship]

  def fromStoredInvitation(si: StoredInvitation): AltItsaRelationship =
    AltItsaRelationship(arn = si.arn, dateFrom = Some(si.lastUpdated.toLocalDate))
}

object ItsaRelationship {
  implicit val relationshipWrites: OWrites[ItsaRelationship] = Json.writes[ItsaRelationship]

  implicit val reads: Reads[ItsaRelationship] =
    ((JsPath \ "agentReferenceNumber").read[Arn] and
      (JsPath \ "dateFrom").readNullable[LocalDate])(ItsaRelationship.apply _)

}

case class PirRelationship(arn: Arn, dateFrom: Option[LocalDate]) extends Relationship {
  val serviceName = Services.HMRCPIR
  val isAltItsa = false
}

object PirRelationship {
  implicit val relationshipWrites: OWrites[PirRelationship] = Json.writes[PirRelationship]

  implicit val reads: Reads[PirRelationship] = ((JsPath \ "arn").read[Arn] and
    (JsPath \ "startDate").readNullable[LocalDateTime].map(date => javaDateTimeToJodaDate(date.get)))(PirRelationship.apply _)

  def javaDateTimeToJodaDate(javaTime: LocalDateTime): Option[LocalDate] =
    Some(LocalDate.parse(javaTime.toLocalDate.toString))
}

case class VatRelationship(arn: Arn, dateFrom: Option[LocalDate]) extends Relationship {
  val serviceName = Services.HMRCMTDVAT
  val isAltItsa = false
}

object VatRelationship {
  implicit val relationshipWrites: OWrites[VatRelationship] = Json.writes[VatRelationship]

  implicit val reads: Reads[VatRelationship] =
    ((JsPath \ "agentReferenceNumber").read[Arn] and
      (JsPath \ "dateFrom").readNullable[LocalDate])(VatRelationship.apply _)

}

case class TrustRelationship(arn: Arn, dateFrom: Option[LocalDate]) extends Relationship {
  val serviceName = Services.TRUST
  val isAltItsa = false
}

object TrustRelationship {
  implicit val relationshipWrites: OWrites[TrustRelationship] = Json.writes[TrustRelationship]

  implicit val reads: Reads[TrustRelationship] =
    ((JsPath \ "agentReferenceNumber").read[Arn] and
      (JsPath \ "dateFrom").readNullable[LocalDate])(TrustRelationship.apply _)

}

case class TrustNtRelationship(arn: Arn, dateFrom: Option[LocalDate]) extends Relationship {
  val serviceName = Services.TRUSTNT
  val isAltItsa = false
}

object TrustNtRelationship {
  implicit val relationshipWrites: OWrites[TrustNtRelationship] = Json.writes[TrustNtRelationship]

  implicit val reads: Reads[TrustNtRelationship] =
    ((JsPath \ "agentReferenceNumber").read[Arn] and
      (JsPath \ "dateFrom").readNullable[LocalDate])(TrustNtRelationship.apply _)

}

case class CgtRelationship(arn: Arn, dateFrom: Option[LocalDate]) extends Relationship {
  val serviceName = Services.CGT
  val isAltItsa = false
}

object CgtRelationship {
  implicit val relationshipWrites: OWrites[CgtRelationship] = Json.writes[CgtRelationship]

  implicit val reads: Reads[CgtRelationship] =
    ((JsPath \ "agentReferenceNumber").read[Arn] and
      (JsPath \ "dateFrom").readNullable[LocalDate])(CgtRelationship.apply _)

}

case class PptRelationship(arn: Arn, dateFrom: Option[LocalDate]) extends Relationship {
  val serviceName = Services.PPT
  val isAltItsa = false
}

object PptRelationship {
  implicit val relationshipWrites: OWrites[PptRelationship] = Json.writes[PptRelationship]

  implicit val reads: Reads[PptRelationship] =
    ((JsPath \ "agentReferenceNumber").read[Arn] and
      (JsPath \ "dateFrom").readNullable[LocalDate])(PptRelationship.apply _)

}

case class CbcUKRelationship(arn: Arn, dateFrom: Option[LocalDate]) extends Relationship {
  val serviceName = Services.HMRCCBCORG
  val isAltItsa = false
}

object CbcUKRelationship {
  implicit val relationshipWrites: OWrites[CbcUKRelationship] = Json.writes[CbcUKRelationship]

  implicit val reads: Reads[CbcUKRelationship] =
    ((JsPath \ "agentReferenceNumber").read[Arn] and
      (JsPath \ "dateFrom").readNullable[LocalDate])(CbcUKRelationship.apply _)

}

case class CbcNonUKRelationship(arn: Arn, dateFrom: Option[LocalDate]) extends Relationship {
  val serviceName = Services.HMRCCBCNONUKORG
  val isAltItsa = false
}

object CbcNonUKRelationship {
  implicit val relationshipWrites: OWrites[CbcNonUKRelationship] = Json.writes[CbcNonUKRelationship]

  implicit val reads: Reads[CbcNonUKRelationship] =
    ((JsPath \ "agentReferenceNumber").read[Arn] and
      (JsPath \ "dateFrom").readNullable[LocalDate])(CbcNonUKRelationship.apply _)

}

case class PlrRelationship(arn: Arn, dateFrom: Option[LocalDate]) extends Relationship {
  val serviceName = Services.HMRCPILLAR2ORG
  val isAltItsa = false
}

object PlrRelationship {
  implicit val relationshipWrites: OWrites[PlrRelationship] = Json.writes[PlrRelationship]

  implicit val reads: Reads[PlrRelationship] =
    ((JsPath \ "agentReferenceNumber").read[Arn] and
      (JsPath \ "dateFrom").readNullable[LocalDate])(PlrRelationship.apply _)

}
