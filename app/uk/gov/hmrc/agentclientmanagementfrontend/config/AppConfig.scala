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

package uk.gov.hmrc.agentclientmanagementfrontend.config

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig

@ImplementedBy(classOf[FrontendAppConfig])
trait AppConfig {

  val authBaseUrl: String
  val agentServicesAccountBaseUrl: String
  val agentFiRelationshipBaseUrl: String
  val sessionCacheBaseUrl: String
  val agentClientRelationshipsBaseUrl: String
  val agentClientAuthorisationBaseUrl: String
  val sessionCacheDomain: String
  val configuration: Configuration
  val environment: Environment
  val contactFrontendBaseUrl: String
  val agentInvitationsFrontendBaseUrl: String
  val appName: String
  def warmUpUrl(clientType: String, uid: String, agencyName: String): String
  val contactFrontendUrl: String
  val contactFrontendAjaxUrl: String
  val contactFrontendNonJsUrl: String
  def featuresRemoveAuthorisation(service: String): Boolean
  val googleAnalyticsToken: String
  val googleAnalyticsHost: String
  val loggerDateFormat: String
  val timeout: Int
  val countdown: Int
}


@Singleton
class FrontendAppConfig @Inject()(val configuration: Configuration, val environment: Environment) extends AppConfig with ServicesConfig {

  override lazy val runModeConfiguration: Configuration = configuration
  override lazy val mode: Mode = environment.mode

  override lazy val authBaseUrl: String = baseUrl("auth")
  override lazy val agentServicesAccountBaseUrl: String = baseUrl("agent-services-account")
  override lazy val agentFiRelationshipBaseUrl: String = baseUrl("agent-fi-relationship")
  override lazy val sessionCacheBaseUrl: String = baseUrl("cachable.session-cache")
  override lazy val agentClientRelationshipsBaseUrl: String = baseUrl("agent-client-relationships")
  override lazy val agentClientAuthorisationBaseUrl: String = baseUrl("agent-client-authorisation")
  override lazy val sessionCacheDomain: String = getString("microservice.services.cachable.session-cache.domain")

  override lazy val contactFrontendBaseUrl: String = getString("microservice.services.contact-frontend.external-url")
  override lazy val agentInvitationsFrontendBaseUrl: String = getString("microservice.services.agent-invitations-frontend.external-url")
  override lazy val appName: String = getString("appName")

  private def normaliseAgentName(agentName: String) =
    agentName.toLowerCase().replaceAll("\\s+", "-").replaceAll("[^A-Za-z0-9-]", "")

  def warmUpUrl(clientType: String, uid: String, agencyName: String): String =
    s"$agentInvitationsFrontendBaseUrl/invitations/$clientType/$uid/${normaliseAgentName(agencyName)}"

  override lazy val contactFrontendUrl: String = s"$contactFrontendBaseUrl/contact/problem_reports_"

  override lazy val contactFrontendAjaxUrl: String =
    s"${contactFrontendUrl}ajax?service=$appName"

  override lazy val contactFrontendNonJsUrl: String =
    s"${contactFrontendUrl}nonjs?service=$appName"

  override def featuresRemoveAuthorisation(service: String): Boolean = getConfBooleanOrFail(s"features.remove-authorisation.$service")

  override lazy val googleAnalyticsToken: String = getString("google-analytics.token")

  override lazy val googleAnalyticsHost: String = getString("google-analytics.host")

  lazy val loggerDateFormat: String = getString("logger.json.dateformat")

  override lazy val timeout: Int = getConfIntOrFail("timeoutDialog.timeout-seconds")

  override lazy val countdown: Int = getConfIntOrFail("timeoutDialog.timeout-countdown-seconds")

  private def getConfIntOrFail(key: String): Int =
    configuration.getInt(key).getOrElse(throw new Exception(s"Property not found $key"))

  private def getConfBooleanOrFail(key: String): Boolean =
    configuration.getBoolean(key).getOrElse(throw new Exception(s"Property not found $key"))
}