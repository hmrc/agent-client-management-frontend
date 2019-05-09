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

import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class ErrorHandlerSpec extends UnitSpec with OneAppPerSuite {

  val handler: ErrorHandler = app.injector.instanceOf[ErrorHandler]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "ErrorHandler should show the error page" when {
    "a client error (404) occurs" in {
      val result = handler.onClientError(FakeRequest(), NOT_FOUND, "")

      status(result) shouldBe NOT_FOUND
      contentType(result) shouldBe Some(HTML)
        checkIncludesText(result,"<p>If you typed the web address, check it is correct.</p>")
        checkIncludesMessages(result, "global.error.pageNotFound404.title", "global.error.pageNotFound404.heading")
    }
    "a client error (400) occurs" in {
      val result = handler.onClientError(FakeRequest(), BAD_REQUEST, "")

      status(result) shouldBe BAD_REQUEST
      contentType(result) shouldBe Some(HTML)
      checkIncludesMessages(result, "global.error.badRequest400.title", "global.error.badRequest400.heading","global.error.badRequest400.message")
    }
  }

        private def checkIncludesMessages(result: Future[Result], messageKeys: String*): Unit =
        messageKeys.foreach { messageKey =>
        messagesApi.isDefinedAt(messageKey) shouldBe true
        contentAsString(result) should include(HtmlFormat.escape(messagesApi(messageKey)).toString)
      }

       private def checkIncludesText(result: Future[Result], messageKeys: String*): Unit =
        messageKeys.foreach { messageKey =>
        contentAsString(result) should include(messageKey.toString)
    }
}
