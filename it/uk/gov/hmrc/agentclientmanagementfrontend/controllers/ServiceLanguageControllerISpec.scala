package uk.gov.hmrc.agentclientmanagementfrontend.controllers

import play.api.test.FakeRequest
import uk.gov.hmrc.agentclientmanagementfrontend.support.BaseISpec
import play.api.test.Helpers.{cookies, redirectLocation}

import scala.concurrent.duration._

class ServiceLanguageControllerISpec extends BaseISpec {

  lazy private val controller: ServiceLanguageController = app.injector.instanceOf[ServiceLanguageController]

  implicit private val timeout = 2.seconds

  "GET /language/:lang" should {

    val request = FakeRequest("GET", "/language/english")

    "redirect to https://www.gov.uk/fallback when the request header contains no referer" in {

      val result = controller.switchToLanguage("english")(request)
      status(result) shouldBe 303
      redirectLocation(result)(timeout) shouldBe Some("https://www.tax.service.gov.uk/manage-your-tax-agents/")

      cookies(result)(timeout).get("PLAY_LANG").get.value shouldBe "en"

    }

    "redirect to /some-page when the request header contains referer /some-page" in {

      val request = FakeRequest("GET", "/language/english").withHeaders("referer" -> "/some-page")

      val result = controller.switchToLanguage("english")(request)
      status(result) shouldBe 303
      redirectLocation(result)(timeout) shouldBe Some("/some-page")

      cookies(result)(timeout).get("PLAY_LANG").get.value shouldBe "en"

    }
  }

}
