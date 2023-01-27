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

package uk.gov.hmrc.agentclientmanagementfrontend

import uk.gov.hmrc.agentclientmanagementfrontend.util.Services._
import uk.gov.hmrc.agentmtdidentifiers.model._
import uk.gov.hmrc.domain.{Nino, TaxIdentifier}

import scala.concurrent.Future

package object util {

  implicit def toFuture[A](a: A): Future[A] = Future.successful(a)

  implicit class taxIdentifierOps(taxIdentifier: TaxIdentifier) {
    val serviceKey: String = taxIdentifier match {
      case _: Nino   => HMRCMTDIT
      case _: Vrn    => HMRCMTDVAT
      case _: Utr    => TRUST
      case _: Urn    => TRUSTNT
      case _: CgtRef => CGT
      case _: PptRef => PPT
      case _ =>
        throw new IllegalStateException(s"Unsupported Identifier $taxIdentifier")
    }
  }

}
