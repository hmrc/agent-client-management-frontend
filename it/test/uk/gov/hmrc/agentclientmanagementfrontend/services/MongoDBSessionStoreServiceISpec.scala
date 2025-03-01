/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmanagementfrontend.models._
import uk.gov.hmrc.agentclientmanagementfrontend.repository.SessionCacheRepository
import uk.gov.hmrc.agentclientmanagementfrontend.support.BaseISpec
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, SymmetricCryptoFactory}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.mongo.CurrentTimestampSupport
import uk.gov.hmrc.mongo.cache.CacheItem
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport

import java.time._
import scala.concurrent.ExecutionContext.Implicits.global

class MongoDBSessionStoreServiceISpec extends BaseISpec with GuiceOneAppPerSuite with CleanMongoCollectionSupport with IntegrationPatience {

  private implicit val crypto: Encrypter with Decrypter = SymmetricCryptoFactory.aesCrypto(
    "td5GaqQ/bDk47dDWzhchchAT03xpFoUy1wb+YOoA/IM="
  )

  override lazy val app = GuiceApplicationBuilder()
    .configure(
      "mongodb.uri"      -> mongoUri,
      "metrics.enabled"  -> false,
      "auditing.enabled" -> false,
      "metrics.jvm"      -> false
    )
    .build()

  private val mongoSessionCacheRepository = new SessionCacheRepository(mongoComponent, new CurrentTimestampSupport)
  private val mongoDBSessionStoreService = new MongoDBSessionStoreService(mongoSessionCacheRepository)

  private val id: String = "sessionId123456"
  private val localDate: LocalDate = LocalDate.now()

  private val clientCache: ClientCache = ClientCache(
    uuId = "dc89f36b64c94060baa3ae87d6b7ac09next",
    arn = Arn("ABCD123456"),
    agencyName = "Some Agency Name",
    service = "service",
    dateAuthorised = Some(localDate),
    isAltItsa = true
  )

  private val clientCacheJson: JsValue = Json.parse(
    s"""
       |{
       |    "clientCache": [{
       |            "uuId": "dc89f36b64c94060baa3ae87d6b7ac09next",
       |            "arn": "PGfyBMDAef8jTJSiq5YpaQ==",
       |            "agencyName": "1bXYGi7wN6Z60zrMH7GLGdibo2SH2diBAXsykkPJdrY=",
       |            "service": "service",
       |            "dateAuthorised": "$localDate",
       |            "isAltItsa": true
       |    }]
       |}
    """.stripMargin
  )

  "MongoDBSessionStoreService" when {
    "session ID is present" should {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(id)))

      "return the cached data when session data" which {
        "is encrypted has been stored" in {
          await(mongoDBSessionStoreService.storeClientCache(Seq(clientCache)))

          val result: Seq[CacheItem] = await(mongoSessionCacheRepository.collection.find().toFuture())

          result.size shouldBe 1

          result.head.data shouldBe clientCacheJson

          await(mongoDBSessionStoreService.fetchClientCache) shouldBe Some(Seq(clientCache))
        }
      }

      "return None when no session data has been stored" in {
        await(mongoDBSessionStoreService.fetchClientCache) shouldBe None
      }

      "remove the underlying storage for the current session when remove session is called" in {
        await(mongoDBSessionStoreService.storeClientCache(Seq(clientCache)))

        await(mongoSessionCacheRepository.collection.find().toFuture()).size shouldBe 1

        await(mongoDBSessionStoreService.remove())

        await(mongoSessionCacheRepository.collection.find().toFuture()).head.data shouldBe Json.obj()
      }

      "session ID is absent" should {
        implicit val hc: HeaderCarrier = HeaderCarrier()

        "return RuntimeException" when {
          "caching session" in {
            intercept[RuntimeException] {
              await(mongoDBSessionStoreService.storeClientCache(Seq(clientCache)))
            }.getMessage shouldBe "Could not store session as no session Id found."
          }
        }

        "return None" when {
          "fetching session" in {
            await(mongoDBSessionStoreService.fetchClientCache) shouldBe None
          }

          "return Unit" when {
            "removing session" in {
              await(mongoDBSessionStoreService.remove()) shouldBe (): Unit
            }
          }
        }
      }
    }
  }
}
