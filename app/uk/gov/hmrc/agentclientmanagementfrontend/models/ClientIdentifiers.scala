/*
 * Copyright 2022 HM Revenue & Customs
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

import uk.gov.hmrc.agentmtdidentifiers.model.{CgtRef, MtdItId, Urn, Utr, Vrn}
import uk.gov.hmrc.domain.Nino

case class ClientIdentifiers(mtdItId: Option[MtdItId], nino:Option[Nino], vrn:Option[Vrn], utr: Option[Utr], cgtRef: Option[CgtRef], urn: Option[Urn]) {
  val haveAtLeastOneFieldDefined: Boolean = mtdItId.isDefined || nino.isDefined || vrn.isDefined || utr.isDefined || cgtRef.isDefined || urn.isDefined
  val hasOnlyNino: Boolean = mtdItId.isEmpty && nino.isDefined && vrn.isEmpty && utr.isEmpty && cgtRef.isEmpty && urn.isEmpty
}
