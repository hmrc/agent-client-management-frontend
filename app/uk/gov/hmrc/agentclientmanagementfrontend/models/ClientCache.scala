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

import java.time.LocalDate
import play.api.libs.functional.syntax.{toFunctionalBuilderOps, toInvariantFunctorOps, unlift}
import play.api.libs.json.{Format, Json, OFormat, __}
import uk.gov.hmrc.crypto.json.JsonEncryption.stringEncrypterDecrypter
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn

case class ClientCache(uuId: String, arn: Arn, agencyName: String, service: String, dateAuthorised: Option[LocalDate], isAltItsa: Boolean = false)

object ClientCache {
  def clientCacheDatabaseFormat(implicit crypto: Encrypter with Decrypter): Format[ClientCache] =
    (
      (__ \ "uuId")
        .format[String] and
        (__ \ "arn")
          .format[String](stringEncrypterDecrypter)
          .inmap[Arn](Arn(_), _.value) and
        (__ \ "agencyName")
          .format[String](stringEncrypterDecrypter) and
        (__ \ "service")
          .format[String] and
        (__ \ "dateAuthorised")
          .formatNullable[LocalDate] and
        (__ \ "isAltItsa")
          .format[Boolean]
    )(ClientCache.apply, unlift(ClientCache.unapply))
  implicit val format: OFormat[ClientCache] = Json.format[ClientCache]
}
