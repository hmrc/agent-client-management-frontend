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

package uk.gov.hmrc.agentclientmanagementfrontend.controllers

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._
import play.api.{Configuration, Environment}
import play.twirl.api.Html
import uk.gov.hmrc.agentclientmanagementfrontend.config.AppConfig
import uk.gov.hmrc.agentclientmanagementfrontend.connectors.PirRelationshipConnector
import uk.gov.hmrc.agentclientmanagementfrontend.services.{AgentClientAuthorisationService, DeleteResponse, RelationshipManagementService}
import uk.gov.hmrc.agentclientmanagementfrontend.util.Services
import uk.gov.hmrc.agentclientmanagementfrontend.views.AuthorisedAgentsPageConfig
import uk.gov.hmrc.agentclientmanagementfrontend.views.html._
import uk.gov.hmrc.auth.core.{AuthConnector, InsufficientEnrolments}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

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
  val env: Environment,
  pirRelationshipConnector: PirRelationshipConnector,
  relationshipManagementService: RelationshipManagementService,
  cc: MessagesControllerComponents,
  val authConnector: AuthConnector,
  authorisedAgentsView: authorised_agents,
  showRemoveAuthView: show_remove_authorisation,
  authorisationsRemoved: authorisation_removed,
  timedOutView: timed_out,
  errorTemplateView: error_template,
  agentClientAuthorisationService: AgentClientAuthorisationService)(
  implicit val appConfig: AppConfig,
  val config: Configuration,
  ec: ExecutionContext)
    extends FrontendController(cc)
    with I18nSupport
    with AuthActions {

  def root(): Action[AnyContent] = Action.async { implicit request =>
    implicit val now: LocalDate = LocalDate.now()
    implicit val dateOrdering: Ordering[LocalDate] = Ordering.fromLessThan(_ isAfter _)
    withAuthorisedAsClient { (clientType, clientIds, _) =>
      for {
        agentRequests <- agentClientAuthorisationService.getAgentRequests(clientType, clientIds)
        authRequests  <- relationshipManagementService.getAuthorisedAgents(clientIds)
        deAuthed <- relationshipManagementService.getDeAuthorisedAgents(clientIds)
        refined   = relationshipManagementService.matchAndRefineStatus(agentRequests.toList, deAuthed.toList)
      } yield Ok(authorisedAgentsView(AuthorisedAgentsPageConfig(authRequests, refined)))
    }
  }

  def showRemoveAuthorisation(id: String): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsClient { (_, _, _) =>
        relationshipManagementService.getAuthorisedAgentDetails(id).map {
          case Some((agencyName, service, _)) => Ok(showRemoveAuthView(RadioConfirm.confirmRadioForm, agencyName, service, id))
          case _                     => redirectToRoot
        }
    }
  }

  def submitRemoveAuthorisation(id: String): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsClient { (_, clientIds, _) =>
      relationshipManagementService.getAuthorisedAgentDetails(id).flatMap {
        case Some((_, service, isAltItsa)) =>
          def response =
            service match {
              case Services.HMRCMTDIT if isAltItsa =>
                relationshipManagementService.deleteAltItsaRelationship(id, clientIds.nino.getOrElse(throw new InsufficientEnrolments))

              case Services.HMRCPIR =>
                relationshipManagementService
                  .deletePIRelationship(id, clientIds.nino.getOrElse(throw new InsufficientEnrolments))

              case _ =>
                relationshipManagementService
                  .deleteRelationship(id, clientIds, service)
            }

            validateRemoveAuthorisationForm(id) {
              response.map {
                case DeleteResponse(true, agencyName, service) =>
                  Redirect(routes.ClientRelationshipManagementController.authorisationRemoved)
                    .addingToSession(("agencyName", agencyName), ("service", service))
                case _ => throw new RuntimeException("relationship deletion failed")
              }
            }
        case _                     => Future successful redirectToRoot
      }
    }
  }

  def authorisationRemoved: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsClient { (_, _, maybeLegacySaUtr) =>
      (request.session.get("agencyName"), request.session.get("service")) match {
        case (Some(agencyName), Some(service)) =>
          service match {
            case "HMRC-MTD-IT" => maybeLegacySaUtr.fold(Future successful(
              Ok(authorisationsRemoved(agencyName, service, None)))){
              utr => pirRelationshipConnector.legacyActiveSaRelationshipExists(utr).map{ hasRelationship =>
                if(hasRelationship) Ok(authorisationsRemoved(agencyName, service, Some(utr.value)))
                else Ok(authorisationsRemoved(agencyName, service, None))}
            }
            case _    => Future successful Ok(authorisationsRemoved(agencyName, service, None))
          }
        case _                                 => Future.successful(redirectToRoot)
      }
    }
  }

  def keepAlive: Action[AnyContent] = Action.async {
    Future.successful(Ok("OK"))
  }

  def signOut: Action[AnyContent] = Action.async {
    startNewSession
  }

  def signOutAndRedirectToTaxAccountRouter: Action[AnyContent] = Action.async{
    Future successful Redirect(appConfig.taxAccountRouterSignInUrl).withNewSession
  }

  def timedOut: Action[AnyContent] = Action.async { implicit request =>
    Future successful Forbidden(timedOutView())
  }

  private def startNewSession: Future[Result] =
    Future.successful(Redirect(routes.ClientRelationshipManagementController.root).withNewSession)

  private def validateRemoveAuthorisationForm(id: String)(serviceCall: => Future[Result])(
    implicit request: Request[AnyContent]): Future[Result] =
    RadioConfirm.confirmRadioForm
      .bindFromRequest()
      .fold(
        formWithErrors =>
          relationshipManagementService.getAuthorisedAgentDetails(id).map {
            case Some((agencyName, service, _)) => Ok(showRemoveAuthView(formWithErrors, agencyName, service, id))
            case _                           => redirectToRoot
        },
        form =>
          if (form.value.getOrElse(false))
            serviceCall
          else
            Future.successful(redirectToRoot)
      )

  private def redirectToRoot =
    Redirect(routes.ClientRelationshipManagementController.root)

  override def forbiddenView(implicit request: Request[_]): Html = errorTemplateView(
    Messages("global.error.403.title"),
    Messages("global.error.403.heading"),
    Messages("global.error.403.message")
  )
}
