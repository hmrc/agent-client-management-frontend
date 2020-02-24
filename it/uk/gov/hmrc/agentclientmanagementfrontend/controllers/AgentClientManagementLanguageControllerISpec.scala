package uk.gov.hmrc.agentclientmanagementfrontend.controllers

import play.api.test.FakeRequest
import uk.gov.hmrc.agentclientmanagementfrontend.support.BaseISpec
import play.api.test.Helpers.redirectLocation
import scala.concurrent.duration._

class AgentClientManagementLanguageControllerISpec extends BaseISpec {

  lazy private val controller: AgentClientManagementLanguageController = app.injector.instanceOf[AgentClientManagementLanguageController]

  implicit private val timeout = 2.seconds

  "GET /language/:lang" should {

    val request = FakeRequest("GET", "/language/english")

    "redirect to https://www.gov.uk/fallback when the request header contains no referer" in {

      val result = controller.switchToLanguage("english")(request)
      status(result) shouldBe 303
      redirectLocation(result)(timeout) shouldBe Some("https://www.gov.uk/fallback")



      //TODO test the cookie value



    }

    "redirect to /some-page when the request header contains referer /some-page" in {

      val request = FakeRequest("GET", "/language/english").withHeaders("referer" -> "/some-page")

      val result = controller.switchToLanguage("english")(request)
      status(result) shouldBe 303
      redirectLocation(result)(timeout) shouldBe Some("/some-page")

    }
  }

}
