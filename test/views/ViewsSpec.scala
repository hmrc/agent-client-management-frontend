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

/*
 * Copyright 2018 HM Revenue & Customs
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

package views

import org.jsoup.Jsoup
import org.scalatestplus.play.MixedPlaySpec
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import support.MockedMetrics
import uk.gov.hmrc.agentclientmanagementfrontend.config.AppConfig
import uk.gov.hmrc.agentclientmanagementfrontend.views.html.error_template_Scope0.error_template_Scope1.error_template
import uk.gov.hmrc.agentclientmanagementfrontend.views.html.govuk_wrapper_Scope0.govuk_wrapper_Scope1.govuk_wrapper
import uk.gov.hmrc.agentclientmanagementfrontend.views.html.main_template_Scope0.main_template_Scope1.main_template

class ViewsSpec extends MixedPlaySpec with MockedMetrics {

  val pageTitle = "My custom page title"
  val heading = "My custom heading"
  val message = "My custom message"

  "error_template view" should {

    "render title, heading and message" in new App {

      val appConf: AppConfig = app.injector.instanceOf[AppConfig]

      val errorTemplate = new error_template

      val html = errorTemplate.render(
        pageTitle = pageTitle,
        heading = heading,
        message = message,
        messages = applicationMessages,
        appConf
        )

      val content = contentAsString(html)

      content must {
        include("My custom page title") and
        include("My custom heading") and
        include("My custom message")
      }

      val html2 =
        new error_template().f("My custom page title", "My custom heading", "My custom message")(
          applicationMessages,
          appConf)
      contentAsString(html2) mustBe (content)
    }
  }

  "main_template view" should {
    "render all supplied arguments" in new App {

      val view = new main_template

      val appConf: AppConfig = app.injector.instanceOf[AppConfig]

      val html = view.render(
        title = pageTitle,
        sidebarLinks = Some(Html("My custom sidebar links")),
        contentHeader = Some(Html("My custom content header")),
        bodyClasses = Some("my-custom-body-class"),
        mainClass = Some("my-custom-main-class"),
        scriptElem = Some(Html("My custom script")),
        mainContent = Html("My custom main content HTML"),
        messages = applicationMessages,
        request = FakeRequest(),
        appConfig = appConf,
        hasTimeout = true)

      val content = contentAsString(html)

      content must {
        include("My custom page title")
          include("My custom sidebar links") and
          include("My custom content header") and
          include("my-custom-body-class") and
          include("my-custom-main-class") and
          include("My custom script") and
          include("My custom main content HTML")
      }

      val doc = Jsoup.parse(contentAsString(html))

      println(doc)

      doc.getElementById("timeoutDialog").isBlock mustBe true


      val html2 = view.f(
        pageTitle,
        Some(Html("My custom sidebar links")),
        Some(Html("My custom content header")),
        Some("my-custom-body-class"),
        Some("my-custom-main-class"),
        Some(Html("My custom script")),true)(Html("My custom main content HTML"))(
        applicationMessages,
        FakeRequest(),
        appConf)
      contentAsString(html2) mustBe (content)
    }
  }

  "govuk wrapper view" should {
    "render all of the supplied arguments" in new App {

      val appConf: AppConfig = app.injector.instanceOf[AppConfig]

      val html = new govuk_wrapper().render(
        title = "My custom page title",
        mainClass = Some("my-custom-main-class"),
        mainDataAttributes = Some(Html("myCustom=\"attributes\"")),
        bodyClasses = Some("my-custom-body-class"),
        sidebar = Html("My custom sidebar"),
        contentHeader = Some(Html("My custom content header")),
        mainContent = Html("My custom main content"),
        serviceInfoContent = Html("My custom service info content"),
        scriptElem = Some(Html("My custom script")),
        gaCode = Seq("My custom GA code"),
        messages = Messages.Implicits.applicationMessages,
        appConfig = appConf,
        hasTimeout = true
      )

      val content = contentAsString(html)

      content must {
        include("My custom page title") and
          include("my-custom-main-class") and
          include("myCustom=\"attributes\"") and
          include("my-custom-body-class") and
          include("My custom sidebar") and
          include("My custom content header") and
          include("My custom main content") and
          include("My custom service info content") and
          include("My custom script")
      }

      val html2 = new govuk_wrapper().f(
        "My custom page title",
        Some("my-custom-main-class"),
        Some(Html("myCustom=\"attributes\"")),
        Some("my-custom-body-class"),
        Html("My custom sidebar"),
        Some(Html("My custom content header")),
        Html("My custom main content"),
        Html("My custom service info content"),
        Some(Html("My custom script")),
        Seq("My custom GA code"),
        true
      )(Messages.Implicits.applicationMessages, appConf)
      contentAsString(html2) mustBe (content)
    }
  }
}

