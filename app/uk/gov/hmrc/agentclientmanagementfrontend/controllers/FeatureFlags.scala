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

package uk.gov.hmrc.agentclientmanagementfrontend.controllers

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentclientmanagementfrontend.config.AppConfig

@Singleton
class FeatureFlags @Inject() (appConfig: AppConfig) {
  val rmAuthIRV: Boolean = appConfig.featuresRemoveAuthorisation("PERSONAL-INCOME-RECORD")
  val rmAuthITSA: Boolean = appConfig.featuresRemoveAuthorisation("HMRC-MTD-IT")
  val rmAuthVAT: Boolean = appConfig.featuresRemoveAuthorisation("HMRC-MTD-VAT")
  val rmAuthTrust: Boolean = appConfig.featuresRemoveAuthorisation("HMRC-TERS-ORG")
  val rmAuthTrustNt: Boolean = appConfig.featuresRemoveAuthorisation("HMRC-TERSNT-ORG")
  val rmAuthCgt: Boolean = appConfig.featuresRemoveAuthorisation("HMRC-CGT-PD")
  val rmAuthPpt: Boolean = appConfig.featuresRemoveAuthorisation("HMRC-PPT-ORG")
}
