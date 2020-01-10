package uk.gov.hmrc.agentclientmanagementfrontend.support

import java.time.LocalDate

import play.api.test.FakeRequest
import play.utils.UriEncoding
import uk.gov.hmrc.agentclientmanagementfrontend.models.{ClientCache, SuspensionDetails}
import uk.gov.hmrc.agentclientmanagementfrontend.stubs._
import uk.gov.hmrc.agentclientmanagementfrontend.util.Services
import uk.gov.hmrc.agentmtdidentifiers.model._
import uk.gov.hmrc.domain.Nino

trait ClientRelationshipManagementControllerTestSetup extends BaseISpec with PirRelationshipStub with AgentClientRelationshipsStub
  with AgentClientAuthorisationStub {

  val mtdItId = MtdItId("ABCDEF123456789")
  val arn1 = Arn("FARN0001131")
  val arn2 = Arn("FARN0001132")
  val arn3 = Arn("FARN0001133")
  val validNino = Nino("AE123456A")
  val validVrn = Vrn("101747641")
  val validUtr = Utr("1977030537")
  val validCgtRef = CgtRef("XMCGTP123456789")
  val startDate = Some(LocalDate.parse("2017-06-06"))
  val startDateString = "2017-06-06"
  val lastUpdated = "2017-01-15T13:14:00.000+08:00"
  val lastUpdatedBefore = "2017-01-05T13:14:00.000+08:00"
  val lastUpdatedAfter = "2017-01-20T13:14:00.000+08:00"
  val cache =
    ClientCache("dc89f36b64c94060baa3ae87d6b7ac08", arn1, "This Agency Name", "Some service name", startDate)
  val cacheItsa =
    ClientCache("dc89f36b64c94060baa3ae87d6b7ac08", arn1, "This Agency Name", "HMRC-MTD-IT", startDate)
  val serviceItsa: String = Services.HMRCMTDIT
  val serviceVat: String = Services.HMRCMTDVAT
  val serviceIrv: String = Services.HMRCPIR
  val serviceTrust: String = Services.TRUST
  val serviceCgt: String = Services.CGT

  //Basic authentication stubs needed for every test
  trait BaseTestSetUp {
    val req = FakeRequest()
    authorisedAsClientAll(req, validNino.nino, mtdItId.value, validVrn.value, validUtr.value, validCgtRef.value)
  }

  //stubs for no relationships for any service
  trait NoRelationshipsFound {
    getNotFoundClientActiveAgentRelationships(serviceItsa)
    getNotFoundForPIRRelationship(serviceIrv, validNino.value)
    getNotFoundClientActiveAgentRelationships(serviceVat)
    getNotFoundClientActiveAgentRelationships(serviceTrust)
    getNotFoundClientActiveAgentRelationships(serviceCgt)
  }

  //stubs for relationships found
  trait RelationshipsFound {
    getClientActiveAgentRelationships(serviceItsa, arn1.value, startDateString)
    getActivePIRRelationship(arn2, serviceIrv, validNino.value, fromCesa = false)
    getClientActiveAgentRelationships(serviceVat, arn3.value, startDateString)
    getNotFoundClientActiveAgentRelationships(serviceTrust)
    getNotFoundClientActiveAgentRelationships(serviceCgt)
    getThreeAgencyNamesMap200(
      (arn1, "abc"),
      (arn2, "DEF"),
      (arn3, "ghi"))
  }

  //stubs for number of pending invitations found 0 1 or 3
  class PendingInvitationsExist(n: Int) {
    require(n == 0 || n == 1 || n == 3)
    n match {
      case 0 =>
        getInvitationsNotFound(mtdItId.value, "MTDITID")
        getInvitationsNotFound(validNino.value, "NI")
        getInvitationsNotFound(validVrn.value, "VRN")

      case 1 =>
        getInvitations(
          arn2,
          validVrn.value,
          "VRN",
          serviceVat,
          "Pending",
          "9999-01-01",
          lastUpdated)
        getInvitationsNotFound(mtdItId.value, "MTDITID")
        getInvitationsNotFound(validNino.value, "NI")
        getAgencyNameMap200(arn2, "abc")

      case 3 =>
        getInvitations(
          arn1,
          validVrn.value,
          "VRN",
          serviceVat,
          "Pending",
          "9999-01-01",
          lastUpdated)
        getInvitations(arn2, mtdItId.value, "MTDITID", serviceItsa, "Pending", "9999-01-01", lastUpdated)
        getInvitations(
          arn3,
          validNino.value,
          "NI",
          serviceIrv,
          "Pending",
          "9999-01-01",
          lastUpdated)
        getThreeAgencyNamesMap200(
          (arn1, "abc"),
          (arn2, "DEF"),
          (arn3, "ghi"))
    }

    getInvitationsNotFound(validUtr.value, "UTR")
    getInvitationsNotFound(validCgtRef.value, "CGTPDRef")
    givenAgentRefExistsFor(arn1)
    givenAgentRefExistsFor(arn2)
    givenAgentRefExistsFor(arn3)
  }

  //stub for when there is invitation history and each record has a different updated date
  trait InvitationHistoryExistsDifferentDates {
    getInvitations(
      arn3,
      validVrn.value,
      "VRN",
      serviceVat,
      "Accepted",
      "9999-01-01",
      lastUpdatedBefore)
    getInvitations(arn1, mtdItId.value, "MTDITID", serviceItsa, "Rejected", "9999-01-01", lastUpdated)
    getInvitations(
      arn2,
      validNino.value,
      "NI",
      serviceIrv,
      "Expired",
      "9999-01-01",
      lastUpdatedAfter)
    getInvitationsNotFound(validUtr.value, "UTR")
    getInvitationsNotFound(validCgtRef.value, "CGTPDRef")
    givenAgentRefExistsFor(arn1)
    givenAgentRefExistsFor(arn2)
    givenAgentRefExistsFor(arn3)
    getThreeAgencyNamesMap200(
      (arn1, "abc"),
      (arn2, "DEF"),
      (arn3, "ghi")
    )
  }

  //stub for when there is invitation history and each record has the same updated date but different time
  trait InvitationHistoryExistsDifferentTimes {
    getInvitations(
      arn1,
      validVrn.value,
      "VRN",
      serviceVat,
      "Accepted",
      "9999-01-01",
      "2017-01-15T13:16:00.000+08:00")
    getInvitations(
      arn2,
      mtdItId.value,
      "MTDITID",
      serviceItsa,
      "Rejected",
      "9999-01-01",
      "2017-01-15T13:15:00.000+08:00")
    getInvitations(
      arn3,
      validNino.value,
      "NI",
      serviceIrv,
      "Expired",
      "9999-01-01",
      "2017-01-15T13:14:00.000+08:00")

    getInvitationsNotFound(validUtr.value, "UTR")
    getInvitationsNotFound(validCgtRef.value, "CGTPDRef")
    givenAgentRefExistsFor(arn1)
    givenAgentRefExistsFor(arn2)
    givenAgentRefExistsFor(arn3)

    getThreeAgencyNamesMap200(
      (arn1, "abc"),
      (arn2, "DEF"),
      (arn3, "ghi")
    )
  }

  //stub for when there is invitation history and each record has the same updated date and time but different names
  trait InvitationHistoryExistsDifferentNames {
    getThreeAgencyNamesMap200((arn1, "abc"), (arn2, "def"), (arn3, "ghi"))

    getInvitations(arn2, validVrn.value, "VRN", serviceVat, "Accepted", "9999-01-01", lastUpdated)
    getInvitations(arn1, mtdItId.value, "MTDITID", serviceItsa, "Rejected", "9999-01-01", lastUpdated)
    getInvitations(arn3, validNino.value, "NI", serviceIrv, "Expired", "2017-01-15", lastUpdated)
    getInvitationsNotFound(validUtr.value, "UTR")
    getInvitationsNotFound(validCgtRef.value, "CGTPDRef")
    givenAgentRefExistsFor(arn1)
    givenAgentRefExistsFor(arn2)
    givenAgentRefExistsFor(arn3)
  }

  trait NoSuspensions {
    givenSuspensionDetails(arn1.value, SuspensionDetails(suspensionStatus = false, None))
    givenSuspensionDetails(arn2.value, SuspensionDetails(suspensionStatus = false, None))
    givenSuspensionDetails(arn3.value, SuspensionDetails(suspensionStatus = false, None))
  }
}
