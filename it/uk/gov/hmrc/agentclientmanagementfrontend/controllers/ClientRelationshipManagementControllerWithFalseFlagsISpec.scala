//package uk.gov.hmrc.agentclientmanagementfrontend.controllers
//
//import play.api.libs.ws.WSClient
//import play.api.test.FakeRequest
//import play.api.test.Helpers._
//import play.utils.UriEncoding
//import uk.gov.hmrc.agentclientmanagementfrontend.models.ClientCache
//import uk.gov.hmrc.agentclientmanagementfrontend.stubs.{AgentClientAuthorisationStub, AgentClientRelationshipsStub, PirRelationshipStub}
//import uk.gov.hmrc.agentclientmanagementfrontend.support.BaseISpec
//import uk.gov.hmrc.agentclientmanagementfrontend.util.Services
//import uk.gov.hmrc.agentmtdidentifiers.model._
//import uk.gov.hmrc.domain.Nino
//import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
//
//import java.time.LocalDate
//import scala.concurrent.ExecutionContext.Implicits.global
//
//class ClientRelationshipManagementControllerWithFalseFlagsISpec extends BaseISpec
//  with PirRelationshipStub
//  with AgentClientRelationshipsStub
//  with AgentClientAuthorisationStub{
//
//  override def featureRemoveAuthorisationPir = false
//
//  override def featureRemoveAuthorisationITSA = false
//
//  override def featureRemoveAuthorisationVat = false
//
//  override def featureRemoveAuthorisationTrust = false
//
//  private lazy val controller: ClientRelationshipManagementController = app.injector.instanceOf[ClientRelationshipManagementController]
//  val wsClient: WSClient = app.injector.instanceOf[WSClient]
//
//  val urlJustWithPrefix = s"http://localhost:$port/manage-your-tax-agents"
//  val doGetRequest = (endOfUrl: String) => wsClient.url(s"$urlJustWithPrefix$endOfUrl").withHttpHeaders(SessionKeys.authToken -> "Bearer XYZ").get()
//
//  def req(method: String = GET) = FakeRequest(method, "/").withSession(SessionKeys.authToken -> "Bearer XYZ")
//  val mtdItId = MtdItId("ABCDEF123456789")
//  val validArn = Arn("FARN0001132")
//  val validNino = Nino("AE123456A")
//  val validVrn =  Vrn("101747641")
//  val validUtr = Utr("1977030537")
//  val validUrn = Urn("ABC12345NT")
//  val validCgtRef = CgtRef("XMCGTP123456789")
//  val validPptRef = PptRef("XAPPT000012345")
//  val startDate = Some(LocalDate.parse("2017-06-06"))
//  val startDateString = "2017-06-06"
//  val serviceItsa = Services.HMRCMTDIT
//  val serviceVat = Services.HMRCMTDVAT
//  val serviceIrv = Services.HMRCPIR
//  val serviceTrust = Services.TRUST
//  val serviceCgt = Services.CGT
//  val serviceNtTrust = Services.TRUSTNT
//  val servicePpt = Services.PPT
//
//  implicit val hc = HeaderCarrier()
//
//  val encodedClientId = UriEncoding.encodePathSegment(mtdItId.value, "UTF-8")
//  val cache = ClientCache("dc89f36b64c94060baa3ae87d6b7ac08", validArn, "This Agency Name", "Some service name", startDate)
//
//  "manageTaxAgents, works as normal except projections of remove authorisation links for false service flag" should {
//    "200, do not show remove authorisation links, other than that works normal" in {
//      authorisedAsClientAll(req(), validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validUrn.value, validCgtRef.value, validPptRef.value)
//      getClientActiveAgentRelationships(serviceItsa, validArn.value, startDateString)
//      getActivePIRRelationship(validArn.copy(value = "FARN0001131"), serviceIrv, validNino.value, fromCesa = false)
//      getClientActiveAgentRelationships(serviceVat, validArn.copy(value = "FARN0001133").value, startDateString)
//      getNAgencyNamesMap200(Map(validArn -> "abc",validArn.copy(value = "FARN0001131")-> "DEF" , validArn.copy(value = "FARN0001133")-> "DEF"))
//      getInvitationsNotFound(validVrn.value, "VRN")
//      getInvitationsNotFound(mtdItId.value, "MTDITID")
//      getInvitationsNotFound(validNino.value, "NI")
//      getAltItsaActiveRelationshipsNotFound(validNino.value)
//      getInvitationsNotFound(validUrn.value, "URN")
//      getInvitationsNotFound(validCgtRef.value, "CGTPDRef")
//      getInvitationsNotFound(validUtr.value, "UTR")
//      getNotFoundClientActiveAgentRelationships(serviceCgt)
//      getNotFoundClientActiveAgentRelationships(serviceTrust)
//      getNotFoundClientActiveAgentRelationships(serviceNtTrust)
//      getInactiveClientRelationshipsEmpty()
//      getInactivePIRRelationshipsEmpty()
//
//      val result = await(doGetRequest(""))
//      result.body.contains("Remove authorisation") shouldBe false
//    }
//  }
//
//  "view removeAuthorisation" should {
//    behave like getRemoveAuthorisationPage(serviceIrv)
//    behave like getRemoveAuthorisationPage(serviceItsa)
//
//    def getRemoveAuthorisationPage(service: String) = {
//      s"return BadRequest for service: $service when flag for service is false" in {
//        authorisedAsClientAll(req(), validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validUrn.value, validCgtRef.value, validPptRef.value)
//
//        val result = await(
//          controller.showRemoveAuthorisation(cache.uuId)(req()))
//
//        status(result) shouldBe 400
//      }
//    }
//  }
//
//  "post removeAuthorisation, BadRequest as flags are false" should {
//    behave like postRemoveAuthorisationForm(serviceItsa)
//    behave like postRemoveAuthorisationForm(serviceIrv)
//
//    def postRemoveAuthorisationForm(service: String) = {
//      s"return BadRequest for attempting to remove relationship when flag for service: $service is false" in {
//
//        sessionStoreService.storeClientCache(Seq(cache))
//        val result = await(controller.submitRemoveAuthorisation(cache.uuId)(authorisedAsClientAll(req(POST), validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validUrn.value, validCgtRef.value, validPptRef.value).withFormUrlEncodedBody("confirmResponse" -> "true")))
//
//        status(result) shouldBe 400
//      }
//    }
//  }
//}
