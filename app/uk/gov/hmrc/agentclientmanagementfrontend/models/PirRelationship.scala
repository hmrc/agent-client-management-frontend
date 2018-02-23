package uk.gov.hmrc.agentclientmanagementfrontend.models

import java.time.LocalDateTime

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn

case class PirRelationship(arn: Arn,
                           service: String,
                           clientId: String,
                           relationshipStatus: Option[RelationshipStatus] = Some(RelationshipStatus.Active),
                           startDate: LocalDateTime,
                           endDate: Option[LocalDateTime],
                           fromCesa: Option[Boolean] = Some(false))

object PirRelationship {
  implicit val relationshipFormat = Json.format[PirRelationship]

  implicit val reads: Reads[PirRelationship] = {
    (
      (JsPath \ "arn").read[Arn] and
        (JsPath \ "service").read[String] and
        (JsPath \ "clientId").read[String] and
        (JsPath \ "relationshipStatus").readNullable[RelationshipStatus] and
        (JsPath \ "startDate").read[LocalDateTime] and
        (JsPath \ "endDate").readNullable[LocalDateTime] and
        (JsPath \ "fromCesa").readNullable[Boolean]) (PirRelationship.apply _)
  }
}
