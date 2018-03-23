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

import play.api.libs.json._
import uk.gov.hmrc.agentclientmanagementfrontend.util.Services
import uk.gov.hmrc.agentmtdidentifiers.model.Arn

sealed trait Relationship extends Product with Serializable {
  val arn: Arn
  val serviceName: String
}

object Relationship {
  implicit val relationshipWrites: Writes[Relationship] =  Writes[Relationship] {
    case itsa: ItsaRelationship => ItsaRelationship.relationshipWrites.writes(itsa)
    case pir: PirRelationship => PirRelationship.relationshipWrites.writes(pir)
  }

  implicit val relationshipReads =
    __.read[ItsaRelationship].map(x => x: Relationship) orElse __.read[PirRelationship].map(x => x: Relationship)
}

case class ItsaRelationship(arn: Arn) extends Relationship {
  val serviceName = Services.HMRCMTDIT
}

object ItsaRelationship {
  implicit val relationshipWrites = Json.writes[ItsaRelationship]

  implicit val reads: Reads[ItsaRelationship] =
    (JsPath \ "agentReferenceNumber").read[Arn].map(arn => ItsaRelationship(arn))

}
case class PirRelationship(arn: Arn) extends Relationship {
  val serviceName = Services.HMRCPIR
}

object PirRelationship {
  implicit val relationshipWrites = Json.writes[PirRelationship]

  implicit val reads: Reads[PirRelationship] = Json.reads[PirRelationship]
}
