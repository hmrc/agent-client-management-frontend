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

package uk.gov.hmrc.agentclientmanagementfrontend.controllers

import play.api.test.FakeRequest
import uk.gov.hmrc.agentclientmanagementfrontend.support.BaseISpec
import play.api.test.Helpers
import play.api.test.Helpers._

import scala.concurrent.duration._

class ServiceLanguageControllerISpec extends BaseISpec {

  lazy private val controller: ServiceLanguageController = app.injector.instanceOf[ServiceLanguageController]

  implicit private val timeout = 2.seconds

  "GET /language/:lang" should {

    val request = FakeRequest("GET", "/language/english")

    "redirect to https://www.tax.service.gov.uk/manage-your-tax-agents/ when the request header contains no referer" in {

      val result = controller.switchToLanguage("english")(request)
      status(result) shouldBe 303
      Helpers.redirectLocation(result)(timeout) shouldBe Some("https://www.tax.service.gov.uk/manage-your-tax-agents/")

      cookies(result)(timeout).get("PLAY_LANG").get.value shouldBe "en"

    }

    "redirect to /some-page when the request header contains referer /some-page" in {

      val request = FakeRequest("GET", "/language/english").withHeaders("referer" -> "/some-page")

      val result = controller.switchToLanguage("english")(request)
      status(result) shouldBe 303
      Helpers.redirectLocation(result)(timeout) shouldBe Some("/some-page")

      cookies(result)(timeout).get("PLAY_LANG").get.value shouldBe "en"

    }
  }

}
