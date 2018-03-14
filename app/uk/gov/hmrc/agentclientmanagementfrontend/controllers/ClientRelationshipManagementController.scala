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

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.agentclientmanagementfrontend.connectors.FrontendAuthConnector
import uk.gov.hmrc.agentclientmanagementfrontend.services.{DeleteResponse, RelationshipManagementService}
import uk.gov.hmrc.agentclientmanagementfrontend.views.html.{authorisation_removed, authorised_agents, show_remove_authorisation}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}

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
                                                        val authConnector: FrontendAuthConnector,
                                                        val env: Environment,
                                                        relationshipManagementService: RelationshipManagementService)(implicit val configuration: Configuration)
  extends FrontendController with I18nSupport with AuthActions {

  def root(): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsClient { _ =>
      relationshipManagementService.getAuthorisedAgents.map(result => Ok(authorised_agents(result)))
    }
  }

  def showRemoveAuthorisation(id: String): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsClient { _ =>
      relationshipManagementService.getAuthorisedAgentDetails(id).map {
        case Some((agencyName, service)) => Ok(show_remove_authorisation(RadioConfirm.confirmRadioForm, agencyName, service, id))
        case _ => throwNoSessionFoundException(s"id $id")
      }
    }
  }

  def submitRemoveAuthorisation(id: String): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsClient { clientId =>
      RadioConfirm.confirmRadioForm.bindFromRequest().fold(
        formWithErrors =>
          relationshipManagementService.getAuthorisedAgentDetails(id).map {
            case Some((agencyName, service)) => Ok(show_remove_authorisation(formWithErrors, agencyName, service, id))
            case _ => throwNoSessionFoundException(s"id $id")
          },
        form =>
          if (form.value.getOrElse(false))
            relationshipManagementService.deleteRelationship(id, clientId).map {
              case DeleteResponse(true, agencyName, service) =>
                Redirect(routes.ClientRelationshipManagementController.authorisationRemoved).withSession(
                  request.session + ("agencyName",agencyName) + ("service", service))
            }
          else
            Future.successful(Redirect(routes.ClientRelationshipManagementController.root()))
      )
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

  private def throwNoSessionFoundException(any: String*) = throw new Exception(s"No session data found for $any")


}

