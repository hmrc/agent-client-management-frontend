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

package uk.gov.hmrc.agentclientmanagementfrontend.services

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.JsValue
import reactivemongo.api.ReadPreference
import reactivemongo.api.commands.{LastError, WriteResult}
import uk.gov.hmrc.agentclientmanagementfrontend.models.ClientCache
import uk.gov.hmrc.agentclientmanagementfrontend.repository.SessionCacheRepository
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.cache.model.{Cache, Id}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.mongo.DatabaseUpdate
import uk.gov.hmrc.play.test.UnitSpec

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class MongoDBSessionStoreServiceSpec extends UnitSpec {

  private implicit val hc = HeaderCarrier(sessionId = Some(SessionId("sessionId123456")))

  def uuId = "0a8f58c84f51475595424176d172f488"

  "SessionStoreService" should {
    "store clientCache" in new Setup {
      val store = new MongoDBSessionStoreService(mockSessionCacheRepository)

      val cache = ClientCache(uuId, Arn("ABCDE123456"), "Some agency name", "Some service name", Some(LocalDate.parse("2017-06-06")))

      await(store.storeClientCache(Seq(cache)))
      verify(mockSessionCacheRepository).createOrUpdate(any[Id], any[String], any[JsValue])
    }

    "return None when no client data have been stored" in new Setup {
      val store = new MongoDBSessionStoreService(mockSessionCacheRepository)
      when(mockSessionCacheRepository.findById(any[Id], any[ReadPreference])(any[ExecutionContext])).thenReturn(Future.successful(None))

      await(store.fetchClientCache) shouldBe None
    }

    "remove the underlying storage for the current session when remove is called" in new Setup {
      val store = new MongoDBSessionStoreService(mockSessionCacheRepository)

      when(mockSessionCacheRepository.removeById(any(), any())(any()))
        .thenReturn(Future.successful(mockWriteResult))

      await(store.remove())

      verify(mockSessionCacheRepository).removeById(any[Id], any())(any[ExecutionContext])
    }
  }

  trait Setup {
    protected val mockSessionCacheRepository: SessionCacheRepository = mock[SessionCacheRepository]
    protected val mockDatabaseUpdate: DatabaseUpdate[Cache] = mock[DatabaseUpdate[Cache]]
    protected val mockLastError: LastError = mock[LastError]
    protected val mockWriteResult: WriteResult = mock[WriteResult]

    when(mockSessionCacheRepository.createOrUpdate(any[Id], any[String], any[JsValue])).thenReturn(Future.successful(mockDatabaseUpdate))
    when(mockDatabaseUpdate.writeResult).thenReturn(mockLastError)
    when(mockLastError.inError).thenReturn(false)
    when(mockWriteResult.writeErrors).thenReturn(Seq.empty)
  }
}
