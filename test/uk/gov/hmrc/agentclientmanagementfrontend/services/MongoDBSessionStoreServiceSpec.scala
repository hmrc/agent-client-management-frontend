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

package uk.gov.hmrc.agentclientmanagementfrontend.services

import org.scalatest.concurrent.IntegrationPatience
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmanagementfrontend.config.AppConfig
import uk.gov.hmrc.agentclientmanagementfrontend.models.ClientCache
import uk.gov.hmrc.agentclientmanagementfrontend.repository.SessionCacheRepository
import uk.gov.hmrc.agentclientmanagementfrontend.support.UnitSpec
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.mongo.CurrentTimestampSupport
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class MongoDBSessionStoreServiceSpec extends UnitSpec
  with GuiceOneAppPerSuite
  with CleanMongoCollectionSupport
  with IntegrationPatience {

  override lazy val app = GuiceApplicationBuilder()
    .configure(
      "mongodb.uri" -> mongoUri,
      "metrics.enabled"  -> false,
      "auditing.enabled" -> false
    ).build()

  implicit lazy val appConfig = app.injector.instanceOf[AppConfig]
  val mongoSessionCacheRepository = new SessionCacheRepository(mongoComponent, new CurrentTimestampSupport)
  val mongoSessionStoreService = new MongoDBSessionStoreService(mongoSessionCacheRepository)

  private implicit val hc = HeaderCarrier(sessionId = Some(SessionId("sessionId123456")))

  def uuId = "0a8f58c84f51475595424176d172f488"

  "SessionStoreService" should {
    "store clientCache" in {

      val cache = ClientCache(uuId, Arn("ABCDE123456"), "Some agency name", "Some service name", Some(LocalDate.parse("2017-06-06")))

      await(mongoSessionStoreService.storeClientCache(Seq(cache)))

      await(mongoSessionCacheRepository.collection.find().toFuture()).size shouldBe 1
    }

    "return None when no client data have been stored" in {

      await(mongoSessionStoreService.fetchClientCache) shouldBe None
    }

    "remove the underlying storage for the current session when remove is called" in {

      val cache = ClientCache(uuId, Arn("ABCDE123456"), "Some agency name", "Some service name", Some(LocalDate.parse("2017-06-06")))

      await(mongoSessionStoreService.storeClientCache(Seq(cache)))

      await(mongoSessionCacheRepository.collection.find().toFuture()).size shouldBe 1

      await(mongoSessionStoreService.remove())

      await(mongoSessionCacheRepository.collection.find().toFuture().map(ci => ci.head)).data shouldBe Json.obj()

    }
  }
}
