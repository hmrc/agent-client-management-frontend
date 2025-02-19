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

package uk.gov.hmrc.agentclientmanagementfrontend.config

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import play.api.i18n.Lang
import play.api.mvc.Call
import uk.gov.hmrc.agentclientmanagementfrontend.controllers.routes
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@ImplementedBy(classOf[FrontendAppConfig])
trait AppConfig {

  val appName: String
  val authBaseUrl: String
  val agentFiRelationshipBaseUrl: String
  val agentClientRelationshipsBaseUrl: String
  val agentClientAuthorisationBaseUrl: String
  val contactFrontendBaseUrl: String
  val agentInvitationsFrontendBaseUrl: String
  val agentClientRelationshipsFrontendBaseUrl: String
  def warmUpUrl(clientType: String, uid: String, agencyName: String): String
  val contactFrontendUrl: String
  val loggerDateFormat: String
  val timeout: Int
  val countdown: Int
  val languageMap: Map[String, Lang]
  val routeToSwitchLanguage: String => Call
  val itemsPerPage: Int
  val taxAccountRouterBaseUrl: String
  val taxAccountRouterSignInUrl: String
  val contactBaseUrl: String
  val contactCheckSARelationshipUrl: String => String
  val mongoDbExpireAfterSeconds: Int
  val languageToggle: Boolean
  val betaFeedbackUrl: String
  val enableCbc: Boolean
  val enablePillar2: Boolean
  val redirectToACRF: Boolean
}

@Singleton
class FrontendAppConfig @Inject() (val servicesConfig: ServicesConfig) extends AppConfig {

  private def baseUrl(serviceName: String) = servicesConfig.baseUrl(serviceName)
  private def getString(config: String) = servicesConfig.getString(config)

  override lazy val authBaseUrl: String = baseUrl("auth")
  override lazy val agentFiRelationshipBaseUrl: String = baseUrl("agent-fi-relationship")
  override lazy val agentClientRelationshipsBaseUrl: String = baseUrl("agent-client-relationships")
  override lazy val agentClientAuthorisationBaseUrl: String = baseUrl("agent-client-authorisation")
  override val agentClientRelationshipsFrontendBaseUrl: String = getString("microservice.services.agent-client-relationships-frontend.external-url")
  override val contactFrontendBaseUrl: String = getString("microservice.services.contact-frontend.external-url")
  override val agentInvitationsFrontendBaseUrl: String = getString("microservice.services.agent-invitations-frontend.external-url")
  override val appName: String = getString("appName")

  private def normaliseAgentName(agentName: String) =
    agentName.toLowerCase().replaceAll("\\s+", "-").replaceAll("[^A-Za-z0-9-]", "")

  def warmUpUrl(clientType: String, uid: String, agencyName: String): String =
    s"$agentInvitationsFrontendBaseUrl/invitations/$clientType-taxes/manage-who-can-deal-with-HMRC-for-you/$uid/${normaliseAgentName(agencyName)}"

  override val contactFrontendUrl: String = s"$contactFrontendBaseUrl/contact/problem_reports_"

  lazy val loggerDateFormat: String = getString("logger.json.dateformat")

  override val timeout: Int = getConfIntOrFail("timeoutDialog.timeout-seconds")

  override val countdown: Int = getConfIntOrFail("timeoutDialog.timeout-countdown-seconds")

  override val languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

  override val routeToSwitchLanguage: String => Call = (lang: String) => routes.ServiceLanguageController.switchToLanguage(lang)

  lazy val itemsPerPage: Int = servicesConfig.getInt("pagination.itemsperpage")

  override val taxAccountRouterBaseUrl: String = getString("microservice.services.tax-account-router.external-url")

  override val taxAccountRouterSignInUrl: String = s"$taxAccountRouterBaseUrl/bas-gateway/sign-in?continue_url=/account"

  override val contactBaseUrl: String = getString("microservice.services.contact.external-url")

  override val contactCheckSARelationshipUrl: String => String = (utr: String) => s"$contactBaseUrl/contact/self-assessment/ind/$utr/aboutyou"

  override val mongoDbExpireAfterSeconds: Int = servicesConfig.getInt("mongodb.session.expireAfterSeconds")

  private def getConfIntOrFail(key: String): Int = servicesConfig.getInt(key)

  private def getConfBooleanOrFail(key: String): Boolean = servicesConfig.getBoolean(key)

  val betaFeedbackUrl: String = servicesConfig.getString("betaFeedbackUrl")

  override val languageToggle: Boolean = getConfBooleanOrFail("features.enable-welsh-toggle")

  override val enableCbc: Boolean = getConfBooleanOrFail("features.enable-cbc")

  override val enablePillar2: Boolean = getConfBooleanOrFail("features.enable-pillar2")

  override val redirectToACRF: Boolean = getConfBooleanOrFail("redirect-to-acrf")
}
