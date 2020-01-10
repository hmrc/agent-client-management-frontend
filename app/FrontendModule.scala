/*
 * Copyright 2020 HM Revenue & Customs
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

import akka.actor.ActorSystem
import com.google.inject.AbstractModule
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentclientmanagementfrontend.config.{AppConfig, FrontendAppConfig}
import uk.gov.hmrc.agentclientmanagementfrontend.connectors.FrontendAuthConnector
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http.ws.WSHttp

class FrontendModule extends AbstractModule {

  def configure(): Unit = {

    bind(classOf[AppConfig]).to(classOf[FrontendAppConfig])
    bind(classOf[SessionCache]).to(classOf[AgentClientManagementSessionCache])
    bind(classOf[HttpGet]).to(classOf[HttpVerbs])
    bind(classOf[HttpPost]).to(classOf[HttpVerbs])
    bind(classOf[HttpDelete]).to(classOf[HttpVerbs])
    bind(classOf[AuthConnector]).to(classOf[FrontendAuthConnector])

    ()
  }

}

@Singleton
class HttpVerbs @Inject()(val auditConnector: AuditConnector, val appConfig: AppConfig, val actorSystem: ActorSystem)
  extends HttpGet with HttpPost with HttpPut with HttpPatch with HttpDelete with WSHttp
    with HttpAuditing {

  val appName: String = appConfig.appName
  override val hooks = Seq(AuditingHook)
  override def configuration: Option[Config] = Some(appConfig.configuration.underlying)
}


@Singleton
class AgentClientManagementSessionCache @Inject()(val http: HttpGet with HttpPut with HttpDelete,
                                                  val appConfig: AppConfig
                                                 ) extends SessionCache {


  val appName = appConfig.appName
  override lazy val defaultSource = appName
  val baseUrl = appConfig.sessionCacheBaseUrl
  val domain: String = appConfig.sessionCacheDomain
  override val baseUri: String = baseUrl


}