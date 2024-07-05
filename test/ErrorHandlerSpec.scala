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

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentclientmanagementfrontend.support.{LogCapturing, UnitSpec}

import scala.concurrent.Future

class ErrorHandlerSpec extends UnitSpec with GuiceOneAppPerSuite with LogCapturing {

  override lazy val app = GuiceApplicationBuilder()
    .configure(
      "metrics.enabled"  -> false,
      "metrics.jvm"      -> false,
      "auditing.enabled" -> false
    )
    .build()

  val handler: ErrorHandler = app.injector.instanceOf[ErrorHandler]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val lang: Lang = Lang("en")

  "ErrorHandler should show the error page" when {
    "a client error (404) occurs with log" in {
      withCaptureOfLoggingFrom(handler.theLogger) { logEvents =>
        val result = handler.onClientError(FakeRequest(), NOT_FOUND, "some error")

        status(result) shouldBe NOT_FOUND
        contentType(await(result)) shouldBe Some(HTML)
        checkIncludesText(result, "If you typed the web address, check it is correct.")
        checkIncludesMessages(result, "global.error.pageNotFound404.title", "global.error.pageNotFound404.heading")

        logEvents.count(_.getMessage.contains(s"onClientError some error")) shouldBe 1
      }
    }
    "a client error (400) occurs with log" in {
      withCaptureOfLoggingFrom(handler.theLogger) { logEvents =>
        val result = handler.onClientError(FakeRequest(), BAD_REQUEST, "some error")

        status(result) shouldBe BAD_REQUEST
        contentType(await(result)) shouldBe Some(HTML)
        checkIncludesMessages(result, "global.error.badRequest400.title", "global.error.badRequest400.heading", "global.error.badRequest400.message")

        logEvents.count(_.getMessage.contains(s"onClientError some error")) shouldBe 1
      }
    }
  }

  private def checkIncludesMessages(result: Future[Result], messageKeys: String*): Unit =
    messageKeys.foreach { messageKey =>
      messagesApi.isDefinedAt(messageKey) shouldBe true
      contentAsString(await(result)) should include(HtmlFormat.escape(messagesApi(messageKey)).toString)
    }

  private def checkIncludesText(result: Future[Result], messageKeys: String*): Unit =
    messageKeys.foreach { messageKey =>
      contentAsString(await(result)) should include(messageKey.toString)
    }
}
