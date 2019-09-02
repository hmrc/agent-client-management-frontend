/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.mvc.Results.Forbidden
import play.api.mvc.{Request, Result}
import play.api.{Logger, Mode}
import uk.gov.hmrc.agentclientmanagementfrontend.models.ClientIdentifiers
import uk.gov.hmrc.agentmtdidentifiers.model.{MtdItId, Utr, Vrn}
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects

import scala.concurrent.{ExecutionContext, Future}

trait AuthActions extends AuthorisedFunctions with AuthRedirects {

  private val isDevEnv =
    if (env.mode.equals(Mode.Test)) false
    else config.getString("run.mode").forall(Mode.Dev.toString.equals)

  protected def withAuthorisedAsClient[A](body: (String, ClientIdentifiers) => Future[Result])(
    implicit request: Request[A],
    hc: HeaderCarrier,
    ec: ExecutionContext): Future[Result] =
    authorised(AuthProviders(GovernmentGateway))
      .retrieve(affinityGroup and allEnrolments) {
        case affinityG ~ allEnrols =>
          def clientId(serviceName: String, identifierKey: String): Option[String] =
            allEnrols.getEnrolment(serviceName).flatMap(_.getIdentifier(identifierKey).map(_.value))

          val mtdItId = clientId("HMRC-MTD-IT", "MTDITID").map(MtdItId(_))
          val nino = clientId("HMRC-NI", "NINO").map(Nino(_))
          val vrn = clientId("HMRC-MTD-VAT", "VRN").map(Vrn(_))
          val utr = clientId("HMRC-TERS-ORG", "SAUTR").map(Utr(_))

          val clientIds = ClientIdentifiers(mtdItId, nino, vrn, utr)

          if (clientIds.haveAtLeastOneFieldDefined) {
            affinityG match {
              case Some(Individual)   => body("personal", clientIds)
              case Some(Organisation) => body("business", clientIds)
              case _ =>
                Logger.warn("Client logged in with wrong affinity group")
                Future.successful(Forbidden)
            }
          } else {
            Logger.warn("Logged in client does not have required enrolments")
            Future.successful(Forbidden)
          }
      }
      .recover(handleFailure)

  def handleFailure(implicit request: Request[_]): PartialFunction[Throwable, Result] = {
    case _: NoActiveSession ⇒
      toGGLogin(if (isDevEnv) s"http://${request.host}${request.uri}" else s"${request.path}")

    case _: UnsupportedAuthProvider ⇒
      Logger.warn(s"user logged in with unsupported auth provider")
      Forbidden
  }
}
