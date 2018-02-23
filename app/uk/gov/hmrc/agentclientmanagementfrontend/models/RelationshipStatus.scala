package uk.gov.hmrc.agentclientmanagementfrontend.models

import play.api.libs.json._

sealed trait RelationshipStatus {
  val key: String
}

object RelationshipStatus {
  case object Active extends RelationshipStatus { val key = "ACTIVE" }
  case object Terminated extends RelationshipStatus { val key = "TERMINATED" }

  private implicit val statusWrites =  new Writes[RelationshipStatus] {
    override def writes(status: RelationshipStatus): JsValue = status match {
      case Active => JsString(Active.key)
      case Terminated => JsString(Terminated.key)
      case _ => throw new RuntimeException(s"Unable to parse the status to json: $status")
    }
  }

  private implicit val  statusReads = new Reads[RelationshipStatus] {
    override def reads(json: JsValue): JsResult[RelationshipStatus] = json match {
      case JsString(Active.key) => JsSuccess(Active)
      case JsString(Terminated.key) => JsSuccess(Terminated)
      case _ => throw new RuntimeException(s"Unable to parse the json to status: $json")
    }
  }

  implicit val relationshipStatusFormat = Format(statusReads, statusWrites)
}
