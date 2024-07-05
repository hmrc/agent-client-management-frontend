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

import uk.gov.hmrc.agentmtdidentifiers.model.Service.{HMRCCBCNONUKORG, HMRCCBCORG, HMRCCGTPD, HMRCMTDIT, HMRCMTDVAT, HMRCPILLAR2ORG, HMRCPIR, HMRCPPTORG, HMRCTERSNTORG, HMRCTERSORG}
import uk.gov.hmrc.agentmtdidentifiers.model._
import uk.gov.hmrc.domain.{Nino, TaxIdentifier}
case class ClientIdentifiers(
  mtdItId: Option[MtdItId],
  nino: Option[Nino],
  vrn: Option[Vrn],
  utr: Option[Utr],
  cgtRef: Option[CgtRef],
  urn: Option[Urn],
  pptRef: Option[PptRef],
  cbcUkRef: Option[CbcId],
  cbcNonUkRef: Option[CbcId],
  plrId: Option[PlrId]
) {
  val haveAtLeastOneFieldDefined: Boolean =
    mtdItId.isDefined || nino.isDefined || vrn.isDefined || utr.isDefined || cgtRef.isDefined || urn.isDefined || pptRef.isDefined || cbcUkRef.isDefined || cbcNonUkRef.isDefined || plrId.isDefined
  val hasOnlyNino: Boolean =
    nino.isDefined && mtdItId.isEmpty && vrn.isEmpty && utr.isEmpty && cgtRef.isEmpty && urn.isEmpty && pptRef.isEmpty && cbcUkRef.isEmpty && cbcNonUkRef.isEmpty && plrId.isEmpty
  def getIdentifierForService(service: String): Option[TaxIdentifier] =
    service match {
      case HMRCPIR         => nino
      case HMRCMTDIT       => mtdItId
      case HMRCMTDVAT      => vrn
      case HMRCCBCORG      => cbcUkRef
      case HMRCCBCNONUKORG => cbcNonUkRef
      case HMRCCGTPD       => cgtRef
      case HMRCTERSORG     => utr
      case HMRCTERSNTORG   => urn
      case HMRCPPTORG      => pptRef
      case HMRCPILLAR2ORG  => plrId
      case unsupported     => throw new RuntimeException(s"unsupported service $unsupported")
    }
}
