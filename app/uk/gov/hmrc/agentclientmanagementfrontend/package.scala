/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc

import uk.gov.hmrc.agentmtdidentifiers.model.{CgtRef, MtdItId, Utr, Vrn}
import uk.gov.hmrc.domain.{Nino, TaxIdentifier}

package object agentclientmanagementfrontend {

  implicit class TaxIdentifierOps(taxId: TaxIdentifier) {

    def getIdTypeForAca: String = {
      taxId match {
        case _: MtdItId => "MTDITID"
        case _: Vrn => "VRN"
        case _: Nino => "NI"
        case _: Utr => "UTR"
        case _: CgtRef => "CGTPDRef"
      }
    }

    def getIdTypeForAcr: String = {
      taxId match {
        case _: MtdItId => "MTDITID"
        case _: Vrn => "VRN"
        case _: Utr => "SAUTR"
        case _: CgtRef => "CGTPDRef"
      }
    }

    def getServiceKey: String = {
      taxId match {
        case _: MtdItId => "HMRC-MTD-IT"
        case _: Vrn => "HMRC-MTD-VAT"
        case _: Utr => "HMRC-TERS-ORG"
        case _: CgtRef => "HMRC-CGT-PD"
      }
    }

    def getGrafanaId: String = {
      taxId match {
        case _: MtdItId => "ITSA"
        case _: Vrn => "VAT"
        case _: Nino => "IRV"
        case _: Utr => "Trust"
        case _: CgtRef => "CGT"
      }
    }
  }

}
