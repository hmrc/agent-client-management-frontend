package uk.gov.hmrc.agentclientmanagementfrontend.controllers
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.test.unigration.UserFixture
import uk.gov.hmrc.test.unigration.behaviours.AuthenticationBehaviours
import uk.gov.hmrc.test.unigration.specs.ControllerUnigrationSpec

class UnigrationTest extends ControllerUnigrationSpec with AuthenticationBehaviours {

  val pirEnrolment = Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", "AB123456A")), "Activated", None)
  val itsaEnrolment = Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "ABCDEF123456789")), "Activated", None)
  val vatEnrolment = Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "101747641")), "Activated", None)

  "GET /remove-authorisation" should {
    "return Ok" in withSignedInUser(
      UserFixture(
        Credentials("12345", "GovernmentGateway"),
        Name(Some("Percy"), Some("Pig")),
        None,
        Some(Individual),
        None,
        Enrolments(Set(pirEnrolment, itsaEnrolment, vatEnrolment)))) { (headers, session, tags) =>
      withRequest(
        "GET",
        routes.ClientRelationshipManagementController
          .showRemoveAuthorisation("PERSONAL-INCOME-RECORD", "dc89f36b64c94060baa3ae87d6b7ac08")
          .url, headers = headers, session = session + (SessionKeys.authToken -> "Bearer XYZ"), tags = tags) { result =>
        wasOk(result)
      }
    }
  }

}
