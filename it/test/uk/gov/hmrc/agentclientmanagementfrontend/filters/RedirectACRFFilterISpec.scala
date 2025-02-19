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

package uk.gov.hmrc.agentclientmanagementfrontend.filters

import filters.RedirectACRFFilter
import play.api.mvc.Results.Ok
import play.api.mvc.{RequestHeader, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.agentclientmanagementfrontend.support.BaseISpec

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class RedirectACRFFilterISpec extends BaseISpec {

  override protected def additionalConfiguration: Map[String, Any] =
    super.additionalConfiguration + ("redirect-to-acrf" -> true)

  "RedirectACRFFilter.apply" should {

    val redirectFilter = new RedirectACRFFilter()
    val dummyFunc: RequestHeader => Future[Result] = (_: RequestHeader) => Future.successful(Ok("Done"))

    "/manage-your-tax-agents without params should redirect to ACRF default URL without params" in {
      val requestRequiringSessionId = FakeRequest("GET", "/manage-your-tax-agents")

      val result = Await.result(
        redirectFilter.apply(dummyFunc(_))(
          requestRequiringSessionId
        ),
        Duration.apply(2, TimeUnit.SECONDS)
      )

      result.header.status shouldBe 301
      result.header.headers.get("LOCATION") shouldBe Some("http://localhost:9435/agent-client-relationships/manage-your-tax-agents")
    }

    "/manage-your-tax-agents with params should redirect to ACRF default url with params" in {
      val requestRequiringSessionId = FakeRequest("GET", "/manage-your-tax-agents?source=FAKESOURCE&returnURL=http://FAKERETURNURL")

      val result = Await.result(
        redirectFilter.apply(dummyFunc(_))(
          requestRequiringSessionId
        ),
        Duration.apply(2, TimeUnit.SECONDS)
      )

      result.header.status shouldBe 301
      result.header.headers.get("LOCATION") shouldBe Some(
        "http://localhost:9435/agent-client-relationships/manage-your-tax-agents?source=FAKESOURCE&returnURL=http://FAKERETURNURL"
      )
    }

    "/remove-authorisation/id/:id should redirect to ACRF default url" in {
      val requestRequiringSessionId = FakeRequest("GET", "manage-your-tax-agents/remove-authorisation/id/12345")

      val result = Await.result(
        redirectFilter.apply(dummyFunc(_))(
          requestRequiringSessionId
        ),
        Duration.apply(2, TimeUnit.SECONDS)
      )

      result.header.status shouldBe 301
      result.header.headers.get("LOCATION") shouldBe Some("http://localhost:9435/agent-client-relationships/manage-your-tax-agents")
    }
  }

}
