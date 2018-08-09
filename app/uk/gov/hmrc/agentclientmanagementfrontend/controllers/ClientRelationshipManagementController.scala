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

package uk.gov.hmrc.agentclientmanagementfrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import org.joda.time.LocalDate
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.agentclientmanagementfrontend.config.ExternalUrls
import uk.gov.hmrc.agentclientmanagementfrontend.connectors.FrontendAuthConnector
import uk.gov.hmrc.agentclientmanagementfrontend.services.{AgentClientAuthorisationService, DeleteResponse, RelationshipManagementService}
import uk.gov.hmrc.agentclientmanagementfrontend.util.Services
import uk.gov.hmrc.agentclientmanagementfrontend.views.html.{authorisation_removed, authorised_agents, show_remove_authorisation}
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

case class RadioConfirm(value: Option[Boolean])

object RadioConfirm {
  val validateConfirmRadio: Constraint[Option[Boolean]] = Constraint[Option[Boolean]] { fieldValue: Option[Boolean] =>
    if (fieldValue.isDefined)
      Valid
    else
      Invalid(ValidationError("error.confirmResponse.invalid.description"))
  }
  val confirmRadioForm: Form[RadioConfirm] = Form[RadioConfirm](
    mapping("confirmResponse" -> optional(boolean)
      .verifying(validateConfirmRadio))(RadioConfirm.apply)(RadioConfirm.unapply))
}

@Singleton
class ClientRelationshipManagementController @Inject()(
                                                        override val messagesApi: MessagesApi,
                                                        featureFlags: FeatureFlags,
                                                        val authConnector: FrontendAuthConnector,
                                                        val env: Environment,
                                                        relationshipManagementService: RelationshipManagementService,
                                                        agentClientAuthorisationService: AgentClientAuthorisationService)(implicit val configuration: Configuration, externalUrls: ExternalUrls)
  extends FrontendController with I18nSupport with AuthActions {

  def root(): Action[AnyContent] = Action.async { implicit request =>
    implicit val now: LocalDate = LocalDate.now()
    withAuthorisedAsClient { clientIds =>
      for {
        agentRequests <- agentClientAuthorisationService.getAgentRequests(clientIds)
        authRequests <- relationshipManagementService.getAuthorisedAgents(clientIds)
      }yield Ok(authorised_agents(authRequests, agentRequests))
    }
  }

  def showRemoveAuthorisation(service: String, id: String): Action[AnyContent] = Action.async { implicit request =>
      withAuthorisedAsClient { _ =>
        if (isActiveService(service, featureFlags)) {
          relationshipManagementService.getAuthorisedAgentDetails(id).map {
            case Some((agencyName, _)) => Ok(show_remove_authorisation(RadioConfirm.confirmRadioForm, agencyName, service, id))
            case _ => throwNoSessionFoundException(s"id $id")
          }
        } else Future.successful(BadRequest)
      }
  }

  private def isActiveService(service: String, featureFlags: FeatureFlags): Boolean = {
    service match {
      case Services.HMRCPIR => featureFlags.rmAuthIRV
      case Services.HMRCMTDIT => featureFlags.rmAuthITSA
      case Services.HMRCMTDVAT => featureFlags.rmAuthVAT
      case _ => throw new Exception("Unsupported Service")
    }
  }

  def submitRemoveAuthorisation(service: String, id: String): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsClient { clientIds =>
      def response = {
        service match {
          case Services.HMRCMTDIT => relationshipManagementService.deleteITSARelationship(id, clientIds.mtdItId.getOrElse(throw new InsufficientEnrolments))
          case Services.HMRCPIR => relationshipManagementService.deletePIRelationship(id, clientIds.nino.getOrElse(throw new InsufficientEnrolments))
          case Services.HMRCMTDVAT => relationshipManagementService.deleteVATRelationship(id, clientIds.vrn.getOrElse(throw new InsufficientEnrolments))
          case _ => throw new Exception("Unsupported Service")
        }
      }

      if (isActiveService(service, featureFlags)) {
        validateRemoveAuthorisationForm(id) {
          response.map {
            case DeleteResponse(true, agencyName, `service`) =>
              Redirect(routes.ClientRelationshipManagementController.authorisationRemoved())
                .addingToSession(("agencyName", agencyName), ("service", service))
          }
        }
      } else
        Future.successful(BadRequest)
    }
  }

  def authorisationRemoved: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsClient { _ =>
      (request.session.get("agencyName"), request.session.get("service")) match {
        case (Some(agencyName), Some(service)) => Future.successful(Ok(authorisation_removed(agencyName, service)))
        case _ => throwNoSessionFoundException("agencyName", "service")
      }
    }
  }

  private def validateRemoveAuthorisationForm(id: String)(serviceCall: => Future[Result])(implicit request: Request[AnyContent]): Future[Result] = {
    RadioConfirm.confirmRadioForm.bindFromRequest().fold(
      formWithErrors =>
        relationshipManagementService.getAuthorisedAgentDetails(id).map {
          case Some((agencyName, service)) => Ok(show_remove_authorisation(formWithErrors, agencyName, service, id))
          case _ => throwNoSessionFoundException(s"id $id")
        },
      form =>
        if (form.value.getOrElse(false))
          serviceCall
        else
          Future.successful(Redirect(routes.ClientRelationshipManagementController.root()))
    )
  }

  private def throwNoSessionFoundException(any: String*) = throw new Exception(s"No session data found for $any")
}

