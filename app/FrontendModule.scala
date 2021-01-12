/*
 * Copyright 2021 HM Revenue & Customs
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

import com.google.inject.AbstractModule
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentclientmanagementfrontend.config.AppConfig
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.cache.client.SessionCache

class FrontendModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[SessionCache]).to(classOf[AgentClientManagementSessionCache])
    ()
  }

}


@Singleton
class AgentClientManagementSessionCache @Inject()(val http: HttpClient,
                                                  val appConfig: AppConfig
                                                 ) extends SessionCache {
  val appName = appConfig.appName
  override lazy val defaultSource = appName
  val baseUrl = appConfig.sessionCacheBaseUrl
  val domain: String = appConfig.sessionCacheDomain
  override val baseUri: String = baseUrl
}