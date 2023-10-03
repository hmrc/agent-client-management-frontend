package uk.gov.hmrc.agentclientmanagementfrontend.controllers

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmanagementfrontend.stubs._
import uk.gov.hmrc.agentclientmanagementfrontend.support.{BaseISpec, ClientRelationshipManagementControllerTestSetup, Css}
import uk.gov.hmrc.agentmtdidentifiers.model.Service.{HMRCCBCNONUKORG, HMRCCBCORG, HMRCCGTPD, HMRCMTDIT, HMRCMTDVAT, HMRCPIR, HMRCPPTORG, HMRCTERSNTORG, HMRCTERSORG}
import uk.gov.hmrc.agentmtdidentifiers.model._
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.http.{HeaderCarrier, SessionId, SessionKeys}

import scala.concurrent.ExecutionContext.Implicits.global

class ClientRelationshipManagementControllerISpec
    extends BaseISpec with PirRelationshipStub with AgentClientRelationshipsStub
    with AgentClientAuthorisationStub with ClientRelationshipManagementControllerTestSetup {

  private lazy val controller: ClientRelationshipManagementController =
    app.injector.instanceOf[ClientRelationshipManagementController]

  private implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionId123456")))

  def fakeRequest(method: String = GET) = FakeRequest(method, "/").withSession(SessionKeys.authToken -> "Bearer XYZ")
  "Current requests tab" should {
    "Show tab when pending requests are present with correct number of pending invitations" in new PendingInvitationsExist(3)
      with BaseTestSetUp with NoRelationshipsFound with NoSuspensions with NoInactiveRelationshipsFound  {

      val response = controller.root(None, None)(fakeRequest())

     status(response) shouldBe 200
      val html = Jsoup.parse(contentAsString(response.futureValue))
      //most bizarre title in world but anyways!
      html.title() shouldBe "Manage who can deal with HMRC for you - Manage who can deal with HMRC for you - GOV.UK"
      html.select(Css.H1).text() shouldBe "Manage who can deal with HMRC for you"
      html.select(Css.paragraphs).get(0).text() shouldBe "This page allows you to view and change agent authorisations for:"
      html.select(Css.ulBullet).get(0).select("li").get(0).text() shouldBe "VAT"
      html.select(Css.ulBullet).get(0).select("li").get(1).text() shouldBe "Capital Gains Tax on UK property account"
      html.select(Css.ulBullet).get(0).select("li").get(2).text() shouldBe "A trust or estate"
      html.select(Css.ulBullet).get(0).select("li").get(3).text() shouldBe "Making Tax Digital for Income Tax"
      html.select(Css.ulBullet).get(0).select("li").get(4).text() shouldBe "Plastic Packaging Tax"
      html.select(Css.ulBullet).get(0).select("li").get(5).text() shouldBe "Income record viewer"
      html.select(Css.ulBullet).get(0).select("li").get(6).text() shouldBe "Country-by-country reports"


      checkHtmlResultWithBodyText(
        response.futureValue,
        "Current requests",
        "Agent",
        "Tax service",
        "You need to respond by",
        "What you need to do",
        "1 January 9999",
        "abc",
        "Respond to request",
        "Respond to request ghi View your Income record",
        "Respond to request DEF Manage your Making Tax Digital for Income Tax",
        "Respond to request abc Manage your VAT"
      )
    }

    "Show tab with different message when number of pending invitations is 1" in
      new PendingInvitationsExist(1) with BaseTestSetUp with NoRelationshipsFound with NoSuspensions with NoInactiveRelationshipsFound {
        val response = controller.root(None, None)(fakeRequest())

        status(response) shouldBe 200
        checkHtmlResultWithBodyText(
          response.futureValue,
          "Current requests",
          "Respond to request",
          "Respond to request abc Manage your VAT")
      }

    "Don't show tab when there are no pending invitations" in new PendingInvitationsExist(0) with BaseTestSetUp
    with NoRelationshipsFound with NoSuspensions with NoInactiveRelationshipsFound {
      val response = controller.root(None, None)(fakeRequest())

      status(response) shouldBe 200
      checkHtmlResultWithBodyText(
        response.futureValue,
        "Manage who can deal with HMRC for you"
      )

      checkHtmlResultNotWithBodyText(
        response.futureValue,
        "Current requests",
        "Who sent the request",
        "You need to respond by",
        "What you need to do")
    }

    "Redirect to self when request contains query params source and returnURL and source is PTA" in
      new PendingInvitationsExist(0)
      with BaseTestSetUp
      with NoRelationshipsFound
        with NoSuspensions
        with NoInactiveRelationshipsFound {

        val response = controller.root(Some("PTA"), Some("/somewhere"))(fakeRequest())

        status(response) shouldBe SEE_OTHER
    }

    "Redirect to self when request contains query params source and returnURL and source is BTA" in
      new PendingInvitationsExist(0)
        with BaseTestSetUp
        with NoRelationshipsFound
        with NoSuspensions
        with NoInactiveRelationshipsFound {

        val response = controller.root(Some("BTA"), Some("/somewhere"))(fakeRequest())

        status(response) shouldBe SEE_OTHER
      }

    "Not show backLink when request contains query params source and returnURL and source is not BTA or PTA" in
      new PendingInvitationsExist(0)
        with BaseTestSetUp
        with NoRelationshipsFound
        with NoSuspensions
        with NoInactiveRelationshipsFound {

        val response = controller.root(Some("something"), Some("/somewhere"))(fakeRequest())

        status(response) shouldBe OK

        val html = Jsoup.parse(contentAsString(response.futureValue))

        html.select(Css.backLink).first() shouldBe null
      }

    "Not show backLink when request contains only 1 query param" in
      new PendingInvitationsExist(0)
        with BaseTestSetUp
        with NoRelationshipsFound
        with NoSuspensions
        with NoInactiveRelationshipsFound {

        val response = controller.root(Some("PTA"), None)(fakeRequest())

        status(response) shouldBe OK

        val html = Jsoup.parse(contentAsString(response.futureValue))

        html.select(Css.backLink).first() shouldBe null
      }

    "Show backLink when play session contains source and returnURL data" in
      new PendingInvitationsExist(0)
        with BaseTestSetUp
        with NoRelationshipsFound
        with NoSuspensions
        with NoInactiveRelationshipsFound {

        val response = controller.root(None, None)(fakeRequest().withSession(("myta_src", "PTA"), ("myta_rtn", "/somewhere")))

        status(response) shouldBe OK

        val html = Jsoup.parse(contentAsString(response.futureValue))

        html.select(Css.backLink).text() shouldBe "Back to personal tax account"
      }

    "Ignore invitation when there is no agent reference found for an Arn" in new PendingInvitationsExist(3)
    with BaseTestSetUp with NoSuspensions with NoRelationshipsFound with  NoInactiveRelationshipsFound {
      givenAgentRefNotFoundFor(arn1)

      val response = controller.root(None, None)(fakeRequest())

      status(response) shouldBe 200

      checkHtmlResultWithBodyText(
        response.futureValue,
        "Respond to request ghi View your Income record",
      "Respond to request DEF Manage your Making Tax Digital for Income Tax") //out of 3 show only 2 in the UI due to one missing AgentRef
    }

    "not show a request when the invitation request is for a service for which the agent has subsequently been suspended" in new PendingInvitationsExist(
      3) with BaseTestSetUp with NoRelationshipsFound with NoInactiveRelationshipsFound {

      givenSuspensionDetails(arn1.value, SuspensionDetails(suspensionStatus = true, Some(Set("AGSV"))))
      givenSuspensionDetails(arn2.value, SuspensionDetails(suspensionStatus = false, None))
      givenSuspensionDetails(arn3.value, SuspensionDetails(suspensionStatus = false, None))

      val response = controller.root(None, None)(fakeRequest())

      status(response) shouldBe 200
      val html = Jsoup.parse(contentAsString(response.futureValue))
      //most bizarre title in world but anyways!
      html.title() shouldBe "Manage who can deal with HMRC for you - Manage who can deal with HMRC for you - GOV.UK"
      html.select(Css.H1).text() shouldBe "Manage who can deal with HMRC for you"
      html.select(".govuk-tabs__list-item.govuk-tabs__list-item--selected a").text() shouldBe "Current requests"
      html.select(".govuk-tabs__list-item.govuk-tabs__list-item--selected a").attr("href") shouldBe "#currentRequests"
      val currentRequestsTable: Elements = html.select("table#current-requests-table")
      private val ths: Elements = currentRequestsTable.select("thead tr th")
      ths.get(0).text() shouldBe "Agent"
      ths.get(1).text() shouldBe "Tax service"
      ths.get(2).text() shouldBe "You need to respond by"
      ths.get(3).text() shouldBe "What you need to do"

      private val trs: Elements = currentRequestsTable.select("tbody tr")

      trs.get(0).select("th").get(0).text() shouldBe "ghi"
      private val row1Cells: Elements = trs.get(0).select("td")
      row1Cells.get(0).text() shouldBe "View your Income record"
      row1Cells.get(1).text() shouldBe "1 January 9999"
      row1Cells.get(2).select("span[aria-hidden=true]").text() shouldBe "Respond to request"
      row1Cells.get(2).select("span.govuk-visually-hidden").text() shouldBe "Respond to request ghi View your Income record"

      trs.get(1).select("th").get(0).text() shouldBe "DEF"
      private val row2Cells: Elements = trs.get(1).select("td")
      row2Cells.get(0).text() shouldBe "Manage your Making Tax Digital for Income Tax"
      row2Cells.get(1).text() shouldBe "1 January 9999"
      row2Cells.get(2).select("span[aria-hidden=true]").text() shouldBe "Respond to request"
      row2Cells.get(2).select("span.govuk-visually-hidden").text() shouldBe "Respond to request DEF Manage your Making Tax Digital for Income Tax"


    }

  }

  "Authorised agents tab" should {
    "show tab with authorised agents" in new PendingInvitationsExist(0) with BaseTestSetUp with RelationshipsFound with NoSuspensions with NoInactiveRelationshipsFound {
      val response = controller.root(None, None)(fakeRequest())

      status(response) shouldBe 200
      checkHtmlResultWithBodyText(
        response.futureValue,
        "Manage who can deal with HMRC for you",
        "Authorised agents",
        "Agent",
        "Tax service",
        "When you gave consent",
        "Action",
        "VAT",
        "Making Tax Digital for Income Tax",
        "View your Income record",
        "abc",
        "6 June 2017",
        "Remove authorisation"
      )
      sessionStoreService.currentSession.clientCache.get.size == 3 shouldBe true
    }

    "include partial auth invitations" in {
      authorisedAsClientNi(FakeRequest(),validNino.value)

      getInvitations(arn1, validNino.value, "MTDITID",serviceItsa, "Partialauth", "9999-01-01", lastUpdated)
      getInvitationsNotFound(validNino.value, "NI")

      getAgencyNameMap200(arn1, "abc")
      givenAgentRefExistsFor(arn1)

      getNotFoundForPIRRelationship(serviceIrv, validNino.value)

      getInactiveClientRelationshipsEmpty()
      getInactivePIRRelationshipsEmpty()

      givenSuspensionDetails(arn1.value, SuspensionDetails(suspensionStatus = false, None))

      val response = controller.root(None, None)(fakeRequest())

     status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response.futureValue))
      //most bizarre title in world but anyways!
      html.title() shouldBe "Manage who can deal with HMRC for you - Manage who can deal with HMRC for you - GOV.UK"
      html.select(Css.H1).text() shouldBe "Manage who can deal with HMRC for you"
      html.select(Css.paragraphs).get(0).text() shouldBe "This page allows you to view and change agent authorisations for:"
      html.select(Css.ulBullet).get(0).select("li").get(0).text() shouldBe "VAT"
      html.select(Css.ulBullet).get(0).select("li").get(1).text() shouldBe "Capital Gains Tax on UK property account"
      html.select(Css.ulBullet).get(0).select("li").get(2).text() shouldBe "A trust or estate"
      html.select(Css.ulBullet).get(0).select("li").get(3).text() shouldBe "Making Tax Digital for Income Tax"
      html.select(Css.ulBullet).get(0).select("li").get(4).text() shouldBe "Plastic Packaging Tax"
      html.select(Css.ulBullet).get(0).select("li").get(5).text() shouldBe "Income record viewer"
      html.select(Css.ulBullet).get(0).select("li").get(6).text() shouldBe "Country-by-country reports"
      html.select("a#other-read-guidance").text() shouldBe "For other tax services, read the guidance"
      html.select("a#other-read-guidance").attr("href") shouldBe "https://www.gov.uk/guidance/client-authorisation-an-overview#how-to-change-or-cancel-authorisations-as-an-agent"

      checkHtmlResultWithBodyText(
        response.futureValue,
        "Authorised agents",
        "History",
        "Agent",
        "Tax service",
        "When you gave consent",
        "Manage your Making Tax Digital for Income Tax",
        "15 January 2017",
        "Remove authorisation",
        "Remove authorisation from abc to Manage your Making Tax Digital for Income Tax"
      )
    }

    "Show tab with no authorised agents and different content" in new PendingInvitationsExist(0) with BaseTestSetUp
    with NoRelationshipsFound with NoInactiveRelationshipsFound  {
      val response = controller.root(None, None)(fakeRequest())

      status(response) shouldBe 200
      checkHtmlResultWithBodyText(
        response.futureValue,
        "Manage who can deal with HMRC for you",
        "Authorised agents",
        "You have not appointed someone to deal with HMRC currently.")
    }

    "Show tab with authorised agents when startDate is blank" in new PendingInvitationsExist(0) with BaseTestSetUp with NoInactiveRelationshipsFound {
      getClientActiveAgentRelationshipsNoStartDate(serviceItsa, arn1.value)
      getAgencyNameMap200(arn1, "This Agency Name")
      givenSuspensionDetails(arn1.value, SuspensionDetails(suspensionStatus = false, None))
      getNotFoundClientActiveAgentRelationships(serviceVat)
      getNotFoundClientActiveAgentRelationships(serviceTrust)
      getNotFoundClientActiveAgentRelationships(serviceCgt)
      getNotFoundClientActiveAgentRelationships(serviceTrustNT)
      getNotFoundClientActiveAgentRelationships(servicePpt)
      getNotFoundClientActiveAgentRelationships(serviceCbcUK)
      getNotFoundClientActiveAgentRelationships(serviceCbcNonUK)
      getNotFoundForPIRRelationship(serviceIrv, validNino.value)
      getAltItsaActiveRelationshipsNotFound(validNino.value)

      val response = controller.root(None, None)(fakeRequest())

      status(response) shouldBe 200
      checkHtmlResultWithBodyText(
        response.futureValue,
        "Manage who can deal with HMRC for you",
        "Authorised agents",
        "This Agency Name",
        "Making Tax Digital for Income Tax",
        "Remove authorisation"
      )
      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "if suspension is enabled don't show suspended agents on this tab" in new PendingInvitationsExist(0) with BaseTestSetUp with RelationshipsFound with NoInactiveRelationshipsFound {
      givenSuspensionDetails(arn1.value, SuspensionDetails(suspensionStatus = true, Some(Set("ITSA"))))
      givenSuspensionDetails(arn2.value, SuspensionDetails(suspensionStatus = false, None))
      givenSuspensionDetails(arn3.value, SuspensionDetails(suspensionStatus = false, None))

      val response = controller.root(None, None)(req())

      status(response) shouldBe 200
      checkHtmlResultWithBodyText(
        response.futureValue ,
        "Manage who can deal with HMRC for you",
        "Authorised agents",
        "VAT",
        "View your Income record",
        "6 June 2017",
        "Remove authorisation"
      )
      val doc = Jsoup.parse(contentAsString(response.futureValue))
      val currentAuthsTab: Elements = doc.select("section[id=\"currentAuths\"]")
      currentAuthsTab.contains("abc") shouldBe false
      currentAuthsTab.contains("Manage your Income Tax") shouldBe false
      sessionStoreService.currentSession.clientCache.get.size == 3 shouldBe true
    }

    "500 when getAgencyNames in agent-client-authorisation returns 400 invalid Arn" in new PendingInvitationsExist(0) with  BaseTestSetUp with NoInactiveRelationshipsFound {
      getClientActiveAgentRelationships(serviceItsa, Arn("someInvalidArn").value, startDateString)
      getNotFoundClientActiveAgentRelationships(serviceVat)
      getNotFoundClientActiveAgentRelationships(serviceTrust)
      getNotFoundClientActiveAgentRelationships(serviceCgt)
      getNotFoundClientActiveAgentRelationships(serviceTrustNT)
      getNotFoundClientActiveAgentRelationships(servicePpt)
      getNotFoundClientActiveAgentRelationships(serviceCbcUK)
      getNotFoundClientActiveAgentRelationships(serviceCbcNonUK)
      getNotFoundForPIRRelationship(serviceIrv, validNino.value)
      getAltItsaActiveRelationshipsNotFound(validNino.value)
      getAgencyNamesMap400("someInvalidArn")

      an[Exception] should be thrownBy await(controller.root(None, None)(req()))

      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "500, when getAgencyNames in agent-client-authorisation returns 400 empty Arn" in new PendingInvitationsExist(0) with BaseTestSetUp with NoInactiveRelationshipsFound {
      getClientActiveAgentRelationships(serviceItsa, Arn("").value, startDateString)
      getNotFoundClientActiveAgentRelationships(serviceVat)
      getNotFoundClientActiveAgentRelationships(serviceTrust)
      getNotFoundClientActiveAgentRelationships(serviceCgt)
      getNotFoundClientActiveAgentRelationships(serviceTrustNT)
      getNotFoundClientActiveAgentRelationships(servicePpt)
      getNotFoundClientActiveAgentRelationships(serviceCbcUK)
      getNotFoundClientActiveAgentRelationships(serviceCbcNonUK)
      getNotFoundForPIRRelationship(serviceIrv, validNino.value)
      getAltItsaActiveRelationshipsNotFound(validNino.value)
      getAgencyNamesMap400("")

      an[Exception] should be thrownBy await(controller.root(None, None)(req()))

      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }
  }

  "History tab" should {
    val req = fakeRequest()

    "Show tab for a client with all services and different response scenarios in date order" in new BaseTestSetUp
      with NoRelationshipsFound with InvitationHistoryExistsDifferentDates with NoSuspensions with NoInactiveRelationshipsFound {
      val response = await(controller.root(None, None)(req()))

      status(response) shouldBe 200
      checkHtmlResultWithBodyText(
        response,
        "History",
        "abc",
        "DEF",
        "ghi",
        "Making Tax Digital for Income Tax",
        "View your Income record",
        "VAT",
        "You accepted on:",
        "You declined on:",
        "The request expired on:",
        "15 January 2017",
        "5 January 2017"
      )
      checkHtmlResultNotWithBodyText(response, "05 January 2017")

      val content = contentAsString(response)

      content.indexOf("DEF") < content.indexOf("abc") && content.indexOf("abc") < content
        .indexOf("ghi") shouldBe true
    }

    "Show tab for a client with all services and different response scenarios in time order when dates are the same" in
      new BaseTestSetUp with NoRelationshipsFound with InvitationHistoryExistsDifferentTimes with NoSuspensions with NoInactiveRelationshipsFound {
        val response = await(controller.root(None, None)(req()))

        status(response) shouldBe 200

        val content = contentAsString(response)

        content.indexOf("abc") < content.indexOf("DEF") && content.indexOf("DEF") < content.indexOf(
          "ghi") shouldBe true
      }

    "Show tab for a client with all services and different response scenarios in alphabetical order when dates are the same" in new BaseTestSetUp
      with NoRelationshipsFound with NoInactiveRelationshipsFound with InvitationHistoryExistsDifferentNames with NoSuspensions  {
      val response = await(controller.root(None, None)(req()))

      status(response) shouldBe 200

      val content = contentAsString(response)

      content.indexOf("abc") < content.indexOf("def") && content.indexOf("def") < content.indexOf("ghi") shouldBe true
    }

    "Show tab for a client with all services and different response scenarios in time order when dates are the same with pagination" in
      new BaseTestSetUp with NoRelationshipsFound with NoInactiveRelationshipsFound with InvitationsForPagination {
        val response = await(controller.root(None, None)(req()))

        status(response) shouldBe 200

        val content = contentAsString(response)

        content contains ("Name100") shouldBe true
        content contains ("Name109") shouldBe true
        content  contains ("Name110") shouldBe false
        content contains ("Showing")  shouldBe true
      }


    "Show tab for a client with no relationship history" in new PendingInvitationsExist(0) with BaseTestSetUp with NoRelationshipsFound with NoInactiveRelationshipsFound {
      val response = await(controller.root(None, None)(req()))

      status(response) shouldBe 200
      checkHtmlResultWithBodyText(response, "History", "You do not have any previous activity.")
      sessionStoreService.currentSession.clientCache.get.isEmpty shouldBe true
    }

    "Show tab for a client with no relationship history and has only a Nino enrolment" in new PendingInvitationsExist(0) with NoRelationshipsFound with NoInactiveRelationshipsFound {
      val req = FakeRequest().withSession(SessionKeys.authToken -> "Bearer XYZ")
      authorisedAsClientNi(req,validNino.value)
      val response = await(controller.root(None, None)(req))

        status(response) shouldBe 200
      checkHtmlResultWithBodyText(response, "History", "You do not have any previous activity.")
      sessionStoreService.currentSession.clientCache.get.isEmpty shouldBe true
    }

    "500, when Des returns 400" in {
      authorisedAsClientMtdItId(req, mtdItId.value)
      get400ClientActiveAgentRelationships(serviceItsa)
      getInvitationsNotFound(mtdItId.value, "MTDITID")

      an[Exception] should be thrownBy await(controller.root(None, None)(req))

      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "500, when Des returns 500" in {
      authorisedAsClientMtdItId(req, mtdItId.value)
      get500ClientActiveAgentRelationships(serviceItsa)
      getInvitationsNotFound(mtdItId.value, "MTDITID")

      an[Exception] should be thrownBy await(controller.root(None, None)(req))

      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "500, when Des returns 503" in {
      authorisedAsClientMtdItId(req, mtdItId.value)
      get503ClientActiveAgentRelationships(serviceItsa)
      getInvitationsNotFound(mtdItId.value, "MTDITID")

      an[Exception] should be thrownBy await(controller.root(None, None)(req))

      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "500, when agent-fi-relationship returns 500" in {
      authorisedAsClientNi(req, validNino.nino)
      get500ForPIRRelationship(serviceIrv, validNino.value)
      getInvitationsNotFound(validNino.value, "NI")

      an[Exception] should be thrownBy await(controller.root(None, None)(req))

      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "500, when agent-fi-relationship returns 503" in {
      authorisedAsClientNi(req, validNino.nino)
      get503ForPIRRelationship(serviceIrv, validNino.value)
      getInvitationsNotFound(validNino.value, "NI")

      an[Exception] should be thrownBy await(controller.root(None, None)(req))

      sessionStoreService.currentSession.clientCache.isDefined shouldBe false
    }

    "Show refined statuses that indicate who terminated a relationship when inactive relationships are found" in new BaseTestSetUp
      with NoRelationshipsFound with InvitationHistoryExistsWithInactiveRelationships with NoSuspensions {

      getInactivePIRRelationships(arn2)
      getInactiveClientRelationshipsExist(arn3, arn1)

      val response = await(controller.root(None, None)(req()))

      status(response) shouldBe 200

      val content = contentAsString(response)

      content.contains(s"You removed authorisation on:") shouldBe true
      content.contains(s"HMRC removed authorisation on:") shouldBe true
      content.contains(s"Your agent removed authorised on:") shouldBe true
    }

    "Show refined statuses that indicate who terminated a relationship when inactive relationships and Deauthorised statuses are found" in new BaseTestSetUp
      with NoRelationshipsFound with InvitationHistoryExistsWithInactiveRelationshipsIncludingDeauthedStatus with NoSuspensions {

      getInactivePIRRelationships(arn2)
      getInactiveClientRelationshipsExist(arn3, arn1)

      val response = await(controller.root(None, None)(req()))

      status(response) shouldBe 200

      val content = contentAsString(response)

      content.contains(s"You removed authorisation on:") shouldBe true
      content.contains(s"HMRC removed authorisation on:") shouldBe true
      content.contains(s"Your agent removed authorised on:") shouldBe true
    }
  }

  "showRemoveAuthorisation page" should {

    "return 200 OK and show remove authorisation page for ITSA" in new BaseTestSetUp {
      sessionStoreService.storeClientCache(Seq(cache(HMRCMTDIT)))

      val result = await(controller.showRemoveAuthorisation( "dc89f36b64c94060baa3ae87d6b7ac08")(req()))

      status(result) shouldBe 200
      contentAsString(result).contains("If you remove your authorisation, This Agency Name will no longer be able to manage your Income Tax.") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe true
    }

    "return 200 OK and show remove authorisation page for TERS" in new BaseTestSetUp  {

      sessionStoreService.storeClientCache(Seq(cache(HMRCTERSORG)))

       val result = await(controller.showRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(req()))

      status(result) shouldBe 200
      contentAsString(result).contains("If you remove your authorisation, This Agency Name will no longer be able to maintain a trust or an estate on your behalf.") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe true
    }

    "return 200 OK and show remove authorisation page for TERSNT" in new BaseTestSetUp  {

      sessionStoreService.storeClientCache(Seq(cache(HMRCTERSNTORG)))

      val result = await(controller.showRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(req()))

      status(result) shouldBe 200
      contentAsString(result).contains("If you remove your authorisation, This Agency Name will no longer be able to maintain a trust or an estate on your behalf.") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe true
    }

    "return 200 OK and show remove authorisation page for IRV" in new BaseTestSetUp  {
      sessionStoreService.storeClientCache(Seq(cache(HMRCPIR)))

      val result = await(controller.showRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(req()))

      status(result) shouldBe 200
      contentAsString(result).contains("If you remove your authorisation, This Agency Name will no longer be able to view your income record.") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe true
    }

    "return 200 OK and show remove authorisation page for PPT" in new BaseTestSetUp {
      sessionStoreService.storeClientCache(Seq(cache(HMRCPPTORG)))

      val result = await(controller.showRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(req()))

      status(result) shouldBe 200
      contentAsString(result).contains("If you remove your authorisation, This Agency Name will no longer be able to manage your Plastic Packaging Tax on your behalf.") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe true
    }

    "return 200 OK and show remove authorisation page for CGTPD" in new BaseTestSetUp {
      sessionStoreService.storeClientCache(Seq(cache(HMRCCGTPD)))

      val result = await(controller.showRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(req()))

      status(result) shouldBe 200
      contentAsString(result).contains("If you remove your authorisation, This Agency Name will no longer be able to manage your Capital Gains Tax on UK property account on your behalf.") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe true
    }

    "return 200 OK and show remove authorisation page for VAT" in new BaseTestSetUp {
      sessionStoreService.storeClientCache(Seq(cache(HMRCMTDVAT)))

      val result = await(controller.showRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(req()))

      status(result) shouldBe 200
      contentAsString(result).contains("If you remove your authorisation, This Agency Name will no longer be able to manage your VAT.") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe true
    }

    "return 200 OK and show remove authorisation page for CBC" in new BaseTestSetUp {
      sessionStoreService.storeClientCache(Seq(cache(HMRCCBCORG)))

      val result = await(controller.showRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(req()))

      status(result) shouldBe 200
      contentAsString(result).contains("If you remove your authorisation, This Agency Name will no longer be able to manage your country-by-country reports on your behalf.") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe true
    }

    "return 200 OK and show remove authorisation page for CBCNONUK" in new BaseTestSetUp {
      sessionStoreService.storeClientCache(Seq(cache(HMRCCBCNONUKORG)))

      val result = await(controller.showRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(req()))

      status(result) shouldBe 200
      contentAsString(result).contains("If you remove your authorisation, This Agency Name will no longer be able to manage your country-by-country reports on your behalf.") shouldBe true
      sessionStoreService.currentSession.clientCache.isDefined shouldBe true
    }

    "redirect to /root when an invalid id is passed" in new BaseTestSetUp  {
      sessionStoreService.storeClientCache(Seq(cache))

      val result = await(controller.showRemoveAuthorisation("INVALID_ID")(req()))
      status(result) shouldBe 303
    }

    "redirect to /root when session cache not found" in new BaseTestSetUp  {

      val result = await(controller.showRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(req()))

      status(result) shouldBe 303
    }
  }

  "removeAuthorisations for PERSONAL-INCOME-RECORD" should {

    behave like checkRemoveAuthorisationForService(
      "PERSONAL-INCOME-RECORD",
      deleteActivePIRRelationship(arn1.value, validNino.value, 200))
    val req = FakeRequest(POST, "/").withSession(SessionKeys.authToken -> "Bearer XYZ")

    "return 500  an exception if PIR Relationship is not found" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validUrn.value, validCgtRef.value, validPptRef.value, validCbcUKRef.value, validCbcNonUKRef.value)
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceIrv)))
      deleteActivePIRRelationship(arn1.value, validNino.value, 404)

      an[Exception] should be thrownBy await(
        controller
          .submitRemoveAuthorisation( "dc89f36b64c94060baa3ae87d6b7ac08")(authorisedAsClientAll(
            req,
            validNino.nino,
            mtdItId.value,
            validVrn.value,
            validUtr.value,
            validUrn.value,
            validCgtRef.value,
            validPptRef.value,
            validCbcUKRef.value,
            validCbcNonUKRef.value) withFormUrlEncodedBody ("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "return an exception if PIR relationship service is unavailable" in new BaseTestSetUp {
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceIrv)))
      deleteActivePIRRelationship(arn1.value, validNino.value, 500)

      an[Exception] should be thrownBy await(
        controller
          .submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
            req(POST)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "throw InsufficientEnrolments when Enrolment for chosen service is not found for logged in user" in {
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceIrv)))
      an[InsufficientEnrolments] shouldBe thrownBy {
        await(
          controller.submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
            authorisedAsClientMtdItId(req, mtdItId.value).withFormUrlEncodedBody("confirmResponse" -> "true")))
      }
    }
  }

  "removeAuthorisations for ITSA" should {

    behave like checkRemoveAuthorisationForService(
      serviceItsa,
      deleteActiveITSARelationship(arn1.value, mtdItId.value, 204))
    val req = FakeRequest(POST, "/").withSession(SessionKeys.authToken -> "Bearer XYZ")

    "return 500 a runtime exception if the relationship is not found" in new BaseTestSetUp {

      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceItsa)))
      deleteActiveITSARelationship(arn1.value, mtdItId.value, 404)

      intercept[RuntimeException] {
          await(controller
            .submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
              req(POST)
                .withFormUrlEncodedBody("confirmResponse" -> "true")))
      }.getMessage shouldBe "relationship deletion failed"

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "return an exception if relationship service is unavailable" in new BaseTestSetUp {

      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceItsa)))
      deleteActiveITSARelationship(arn1.value, mtdItId.value, 500)

      an[Exception] should be thrownBy await(
        controller
          .submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
            req(POST)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "throw InsufficientEnrolments when Enrolment for chosen service is not found for logged in user" in {
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceItsa)))
      an[InsufficientEnrolments] shouldBe thrownBy {
        await(
          controller.submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
            authorisedAsClientNi(req, validNino.nino).withFormUrlEncodedBody("confirmResponse" -> "true")))
      }
    }
  }

  "removeAuthorisations for alternative-ITSA" should {

    behave like checkRemoveAuthorisationForService(
      serviceItsa,
      givenSetRelationshipEndedReturns(arn1, validNino.value, 204), true)
    val req = FakeRequest(POST, "/").withSession(SessionKeys.authToken -> "Bearer XYZ")

    "return 500 a runtime exception if the relationship is not found" in new BaseTestSetUp {

      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceItsa)))
      givenSetRelationshipEndedReturns(arn1, validNino.value, 404)

      intercept[RuntimeException] {
        await(controller
          .submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
            req(POST)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))
      }.getMessage shouldBe "relationship deletion failed"

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "return an exception if relationship service is unavailable" in new BaseTestSetUp {

      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceItsa)))
      givenSetRelationshipEndedReturns(arn1, validNino.value, 503)

      an[Exception] should be thrownBy await(
        controller
          .submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
            req(POST)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "throw InsufficientEnrolments when Enrolment for chosen service is not found for logged in user" in {
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceItsa)))
      an[InsufficientEnrolments] shouldBe thrownBy {
        await(
          controller.submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
            authorisedAsClientNi(req, validNino.nino).withFormUrlEncodedBody("confirmResponse" -> "true")))
      }
    }
  }

  "removeAuthorisations for VAT" should {

    behave like checkRemoveAuthorisationForService(
      serviceVat,
      deleteActiveVATRelationship(arn1.value, validVrn.value, 204))
    val req = FakeRequest(POST, "/").withSession(SessionKeys.authToken -> "Bearer XYZ")

    "return 500  an exception if the relationship is not found" in new BaseTestSetUp {
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceVat)))
      deleteActiveVATRelationship(arn1.value, validVrn.value, 404)

      an[Exception] should be thrownBy await(
        controller
          .submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
            req(POST)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "return an exception if relationship service is unavailable" in new BaseTestSetUp {

      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceVat)))
      deleteActiveVATRelationship(arn1.value, validVrn.value, 500)

      an[Exception] should be thrownBy await(
        controller
          .submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
            req(POST)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "throw InsufficientEnrolments when Enrolment for chosen service is not found for logged in user" in {
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceVat)))
      an[InsufficientEnrolments] shouldBe thrownBy {
        await(
          controller.submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
            authorisedAsClientNi(req, validNino.nino).withFormUrlEncodedBody("confirmResponse" -> "true")))
      }
    }

    "return exception if session data not found" in new BaseTestSetUp {
      val request = req(POST).withSession("agencyName" -> cache.agencyName, SessionKeys.authToken -> "Bearer XYZ")
      val result = await(
        controller.submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
          request))
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.ClientRelationshipManagementController.root(None, None).url)
    }
  }

  "removeAuthorisations for Trust" should {

    behave like checkRemoveAuthorisationForService(
      serviceTrust,
      deleteActiveTrustRelationship(arn1.value, validUtr.value, 204))
    val req = FakeRequest(POST, "/").withSession(SessionKeys.authToken -> "Bearer XYZ")

    "return 500  an exception if the relationship is not found" in new BaseTestSetUp {

      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceTrust)))
      deleteActiveVATRelationship(arn1.value, validUtr.value, 404)

      an[Exception] should be thrownBy await(
        controller
          .submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
            req(POST)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "return an exception if relationship service is unavailable" in new BaseTestSetUp {

      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceTrust)))
      deleteActiveVATRelationship(arn1.value, validUtr.value, 500)

      an[Exception] should be thrownBy await(
        controller
          .submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
            req(POST)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "throw InsufficientEnrolments when Enrolment for chosen service is not found for logged in user" in {
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceTrust)))
      an[InsufficientEnrolments] shouldBe thrownBy {
        await(
          controller.submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
            authorisedAsClientNi(req, validNino.nino).withFormUrlEncodedBody("confirmResponse" -> "true")))
      }
    }

    "return exception if session data not found" in new BaseTestSetUp {
      val request = req(POST).withSession("agencyName" -> cache.agencyName, SessionKeys.authToken -> "Bearer XYZ")
      val result = await(
        controller.submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
          request))
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.ClientRelationshipManagementController.root(None, None).url)
    }
  }

  "removeAuthorisations for TrustNT" should {

    behave like checkRemoveAuthorisationForService(
      serviceTrustNT,
      deleteActiveTrustNTRelationship(arn1.value, validUrn.value, 204))
    val req = FakeRequest(POST, "/").withSession(SessionKeys.authToken -> "Bearer XYZ")

    "return 500  an exception if the relationship is not found" in new BaseTestSetUp {

      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceTrustNT)))
      deleteActiveVATRelationship(arn1.value, validUrn.value, 404)

      an[Exception] should be thrownBy await(
        controller
          .submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
            req(POST)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "return an exception if relationship service is unavailable" in new BaseTestSetUp {

      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceTrustNT)))
      deleteActiveVATRelationship(arn1.value, validUrn.value, 500)

      an[Exception] should be thrownBy await(
        controller
          .submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
            req(POST)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "throw InsufficientEnrolments when Enrolment for chosen service is not found for logged in user" in {
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceTrustNT)))
      an[InsufficientEnrolments] shouldBe thrownBy {
        await(
          controller.submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
            authorisedAsClientNi(req, validNino.nino).withFormUrlEncodedBody("confirmResponse" -> "true")))
      }
    }

    "return exception if session data not found" in new BaseTestSetUp {
      val request = req(POST).withSession("agencyName" -> cache.agencyName, SessionKeys.authToken -> "Bearer XYZ")
      val result = await(
        controller.submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
          request))
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.ClientRelationshipManagementController.root(None, None).url)
    }
  }

  "removeAuthorisations for Plastic Packaging Tax" should {

    behave like checkRemoveAuthorisationForService(
      servicePpt,
      deleteActivePptRelationship(arn1.value, validPptRef.value, 204))
    val req = FakeRequest(POST, "/").withSession(SessionKeys.authToken -> "Bearer XYZ")

    "return 500  an exception if the relationship is not found" in new BaseTestSetUp {

      sessionStoreService.storeClientCache(Seq(cache.copy(service = servicePpt)))
      deleteActivePptRelationship(arn1.value, validVrn.value, 404)

      an[Exception] should be thrownBy await(
        controller
          .submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
            req(POST)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "return an exception if relationship service is unavailable" in new BaseTestSetUp  {
      sessionStoreService.storeClientCache(Seq(cache.copy(service = servicePpt)))
      deleteActivePptRelationship(arn1.value, validVrn.value, 500)

      an[Exception] should be thrownBy await(
        controller
          .submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
            req(POST)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "throw InsufficientEnrolments when Enrolment for chosen service is not found for logged in user" in {
      sessionStoreService.storeClientCache(Seq(cache.copy(service = servicePpt)))
      an[InsufficientEnrolments] shouldBe thrownBy {
        await(
          controller.submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
            authorisedAsClientNi(req, validNino.nino).withFormUrlEncodedBody("confirmResponse" -> "true")))
      }
    }

    "return exception if session data not found" in new BaseTestSetUp {
      val request = req(POST).withSession("agencyName" -> cache.agencyName, SessionKeys.authToken -> "Bearer XYZ")
      val result = await(
        controller.submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
          request))
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.ClientRelationshipManagementController.root(None, None).url)
    }
  }

  "removeAuthorisations for Country by country" should {

    behave like checkRemoveAuthorisationForService(
      serviceCbcUK,
      deleteActiveCbcRelationship(arn1.value, validCbcUKRef.value, true, 204))
    val req = FakeRequest(POST, "/").withSession(SessionKeys.authToken -> "Bearer XYZ")

    "return 500  an exception if the relationship is not found" in new BaseTestSetUp {

      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceCbcUK)))
      deleteActiveCbcRelationship(arn1.value, validCbcUKRef.value, true, 404)

      an[Exception] should be thrownBy await(
        controller
          .submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
            req(POST)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "return an exception if relationship service is unavailable" in new BaseTestSetUp  {
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceCbcUK)))
      deleteActiveCbcRelationship(arn1.value, validCbcUKRef.value, true, 500)

      an[Exception] should be thrownBy await(
        controller
          .submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
            req(POST)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "throw InsufficientEnrolments when Enrolment for chosen service is not found for logged in user" in {
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceCbcUK)))
      an[InsufficientEnrolments] shouldBe thrownBy {
        await(
          controller.submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
            authorisedAsClientNi(req, validNino.nino).withFormUrlEncodedBody("confirmResponse" -> "true")))
      }
    }

    "return exception if session data not found" in new BaseTestSetUp {
      val request = req(POST).withSession("agencyName" -> cache.agencyName, SessionKeys.authToken -> "Bearer XYZ")
      val result = await(
        controller.submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
          request))
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.ClientRelationshipManagementController.root(None, None).url)
    }
  }

  "removeAuthorisations for Country by country (non UK)" should {

    behave like checkRemoveAuthorisationForService(
      serviceCbcNonUK,
      deleteActiveCbcRelationship(arn1.value, validCbcNonUKRef.value, false,204))
    val req = FakeRequest(POST, "/").withSession(SessionKeys.authToken -> "Bearer XYZ")

    "return 500  an exception if the relationship is not found" in new BaseTestSetUp {

      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceCbcNonUK)))
      deleteActiveCbcRelationship(arn1.value, validCbcNonUKRef.value, false, 404)

      an[Exception] should be thrownBy await(
        controller
          .submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
            req(POST)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "return an exception if relationship service is unavailable" in new BaseTestSetUp  {
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceCbcNonUK)))
      deleteActiveCbcRelationship(arn1.value, validCbcNonUKRef.value, false, 500)

      an[Exception] should be thrownBy await(
        controller
          .submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
            req(POST)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))

      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "throw InsufficientEnrolments when Enrolment for chosen service is not found for logged in user" in {
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceCbcNonUK)))
      an[InsufficientEnrolments] shouldBe thrownBy {
        await(
          controller.submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
            authorisedAsClientNi(req, validNino.nino).withFormUrlEncodedBody("confirmResponse" -> "true")))
      }
    }

    "return exception if session data not found" in new BaseTestSetUp {
      val request = req(POST).withSession("agencyName" -> cache.agencyName, SessionKeys.authToken -> "Bearer XYZ")
      val result = await(
        controller.submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
          request))
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.ClientRelationshipManagementController.root(None, None).url)
    }
  }

  "authorisationRemoved" should {

    "show authorisation_removed page with required sessions" in new BaseTestSetUp {
      val request = req().withSession("agencyName" -> cacheItsa.agencyName, "service" -> cacheItsa.service, SessionKeys.authToken -> "Bearer XYZ")
      val result = await(
        controller.authorisationRemoved(
          request))

      status(result) shouldBe 200
      checkHtmlResultWithBodyText(
        result,
        "Authorisation removed",
      "What this means",
      "You removed your authorisation for This Agency Name to manage your Making Tax Digital for Income Tax.",
      "If you did not mean to remove your authorisation, ask This Agency Name to send you a new authorisation request link.",
      "If This Agency Name or another agent managed your Self Assessment before Making Tax Digital for Income Tax, you may still have a separate authorisation in place. This means an agent has permission to view and amend your Self Assessment.",
      "To check if you have a separate Self Assessment authorisation in place or to remove it",
        "sign in with your Government Gateway user ID.",
        " It must be the one you used for the Self Assessment you had before Making Tax Digital for Income Tax.")
    }

    "show authorisation_removed page with relevant content when we find the client has an active legacy SA relationship with an agent" in {
      val req = FakeRequest().withSession("agencyName" -> cacheItsa.agencyName, "service" -> cacheItsa.service, SessionKeys.authToken -> "Bearer XYZ")

      getLegacyActiveSaRelationshipExists(validUtr.value)

      val result = await(
        controller.authorisationRemoved(
          authorisedAsClientMtdItIdWithIrSa(req, mtdItId.value, validUtr.value)))

      status(result) shouldBe 200
      checkHtmlResultWithBodyText(
        result,
        "Authorisation removed",
        "What this means",
        "You removed your authorisation for This Agency Name to manage your Making Tax Digital for Income Tax.",
        "If you did not mean to remove your authorisation, ask This Agency Name to send you a new authorisation request link.",
        "You also have an agent authorisation for Self Assessment that is still active. This means an agent has permission to view and amend your Self Assessment. If you want to remove this authorisation as well, you will need to do it separately.",
      "Check or remove active agent authorisation for Self Assessment",
      "Manage who can deal with HMRC for you",
      "Finish and sign out")
    }

    "show authorisation_removed page with relevant content when client has IR-SA enrolment but there is no legacy relationship" in new BaseTestSetUp  {
      val request = req().withSession("agencyName" -> cacheItsa.agencyName, "service" -> cacheItsa.service, SessionKeys.authToken -> "Bearer XYZ")

      getLegacyActiveSaRelationshipExists(validUtr.value, 404)

      val result = await(
        controller.authorisationRemoved(
          authorisedAsClientMtdItIdWithIrSa(request, mtdItId.value, validUtr.value)))

      status(result) shouldBe 200
      checkHtmlResultWithBodyText(
        result,
        "Authorisation removed",
        "What this means",
        "You removed your authorisation for This Agency Name to manage your Making Tax Digital for Income Tax.",
        "If you did not mean to remove your authorisation, ask This Agency Name to send you a new authorisation request link.",
        "If This Agency Name or another agent managed your Self Assessment before Making Tax Digital for Income Tax, you may still have a separate authorisation in place. This means an agent has permission to view and amend your Self Assessment.",
        "To check if you have a separate Self Assessment authorisation in place or to remove it",
        "sign in with your Government Gateway user ID.",
        " It must be the one you used for the Self Assessment you had before Making Tax Digital for Income Tax.")
    }

    "return exception if required session data not found" in new BaseTestSetUp {
      val request = req().withSession("agencyName" -> cache.agencyName, SessionKeys.authToken -> "Bearer XYZ")
      val result = await(
        controller.authorisationRemoved(
          request))
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.ClientRelationshipManagementController.root(None, None).url)
    }
  }

  def checkRemoveAuthorisationForService(serviceName: String, deleteRelationshipStub: => StubMapping, isAltItsa: Boolean = false): Unit = {
    implicit val req = FakeRequest(POST, "/").withSession(SessionKeys.authToken -> "Bearer XYZ")

    "return 200, remove the relationship if the client confirms deletion" in {
      authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validUrn.value, validCgtRef.value, validPptRef.value, validCbcUKRef.value, validCbcNonUKRef.value)

      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceName, isAltItsa = isAltItsa)))

      deleteRelationshipStub

      val result = await(
        controller.submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
          authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validUrn.value, validCgtRef.value, validPptRef.value, validCbcUKRef.value, validCbcNonUKRef.value)
            .withFormUrlEncodedBody("confirmResponse" -> "true")))

      status(result) shouldBe 303


      result.session should not be empty
      result.session.get("agencyName") shouldBe Some(cache.agencyName)
      result.session.get("service") shouldBe Some(serviceName)
    }

    "redirect to manage-your-tax-agents if the client does not confirm deletion" in new BaseTestSetUp  {
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceName)))
      deleteRelationshipStub

      val result = await(
        controller.submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
          req(POST)
            .withFormUrlEncodedBody("confirmResponse" -> "false")))

      status(result) shouldBe 303
      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "show error message if the client does not select any choice from the confirm delete radio buttons" in new BaseTestSetUp {
      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceName)))
      deleteRelationshipStub

      val result = await(
        controller.submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
          req(POST)
            .withFormUrlEncodedBody("confirmResponse" -> "")))

      status(result) shouldBe 200
      checkHtmlResultWithBodyText(result, "Select yes if you want to remove your authorisation.")
      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
    }

    "redirect to root if the session cache is not found" in new BaseTestSetUp {

      deleteRelationshipStub

      val result = await(
        controller
          .submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
            req(POST)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))

      status(result) shouldBe 303

      sessionStoreService.currentSession.clientCache shouldBe empty
    }

    "redirect to /root if an invalid id is submitted" in new BaseTestSetUp {

      sessionStoreService.storeClientCache(Seq(cache.copy(service = serviceName)))
      deleteRelationshipStub

      val result =  await(
        controller
          .submitRemoveAuthorisation("INVALID_ID")(
            req(POST)
              .withFormUrlEncodedBody("confirmResponse" -> "true")))

      status(result) shouldBe 303
    }

    "remove deleted item from the session cache" in new BaseTestSetUp {

      await(sessionStoreService.storeClientCache(
        Seq(
          cache.copy(service = serviceName, isAltItsa = isAltItsa),
          cache.copy(uuId = "dc89f36b64c94060baa3ae87d6b7ac09next", service = serviceName))))
      sessionStoreService.currentSession.clientCache.get.size == 2 shouldBe true

      deleteRelationshipStub

      val result = await(
        controller.submitRemoveAuthorisation("dc89f36b64c94060baa3ae87d6b7ac08")(
          req(POST)
            .withFormUrlEncodedBody("confirmResponse" -> "true")))

      status(result) shouldBe 303
      sessionStoreService.currentSession.clientCache.get.size == 1 shouldBe true
      sessionStoreService.currentSession.clientCache.get.head.uuId shouldBe "dc89f36b64c94060baa3ae87d6b7ac09next"
    }
  }

  "timedOut" should {
    "display the timed out page" in {
      val response = await(controller.timedOut(FakeRequest()))
      status(response) shouldBe 403
      checkHtmlResultWithBodyText(response, "You have been signed out", "so we have signed you out to keep your account secure.")
    }
  }
}
