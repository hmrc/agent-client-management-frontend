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

package uk.gov.hmrc.agentclientmanagementfrontend.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentclientmanagementfrontend.models.ClientCache
import uk.gov.hmrc.agentclientmanagementfrontend.repository.{SessionCache, SessionCacheRepository}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MongoDBSessionStoreService @Inject()(sessionCache: SessionCacheRepository) {

  final val cache = new SessionCache[Seq[ClientCache]] {
    override val sessionName: String = "clientCache"
    override val cacheRepository: SessionCacheRepository = sessionCache
  }

  def fetchClientCache(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Option[Seq[ClientCache]]] = {
    cache.fetch
  }

  def storeClientCache(_cache: Seq[ClientCache])(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Unit] = {
    cache.save(_cache).map(_ => ())
  }

  def remove()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    cache.delete().map(_ => ())
}
