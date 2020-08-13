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

package uk.gov.hmrc.agentclientmanagementfrontend.services

import java.time.LocalDate

import support.TestSessionCache
import uk.gov.hmrc.agentclientmanagementfrontend.models.ClientCache
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class SessionStoreServiceSpec extends UnitSpec {

  private implicit val hc = HeaderCarrier(sessionId = Some(SessionId("sessionId123456")))

  def uuId = "0a8f58c84f51475595424176d172f488"

  "SessionStoreService" should {
    "store clientCache" in {
      val store = new SessionStoreService(new TestSessionCache())

      val cache = ClientCache(uuId, Arn("ABCDE123456"), "Some agency name", "Some service name", Some(LocalDate.parse("2017-06-06")))

      await(store.storeClientCache(Seq(cache)))

      val result = await(store.fetchClientCache)
      result.get shouldBe Seq(cache)
      result.get.head.uuId shouldBe uuId
    }

    "return None when no client data have been stored" in {
      val store = new SessionStoreService(new TestSessionCache())

      await(store.fetchClientCache) shouldBe None
    }

    "remove the underlying storage for the current session when remove is called" in {
      val store = new SessionStoreService(new TestSessionCache())

      val cache = ClientCache(uuId, Arn("ABCDE123456"), "Some agency name", "Some service name", Some(LocalDate.parse("2017-06-06")))

      await(store.storeClientCache(Seq(cache)))
      await(store.remove())

      await(store.fetchClientCache) shouldBe None
    }
  }
}
