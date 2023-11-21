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

import play.api.Logging
import play.api.mvc.Results.{Forbidden, Redirect}
import play.api.mvc.{Request, Result}
import play.twirl.api.Html
import uk.gov.hmrc.agentclientmanagementfrontend.models.ClientIdentifiers
import uk.gov.hmrc.agentmtdidentifiers.model._
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects

import scala.concurrent.{ExecutionContext, Future}

trait AuthActions extends AuthorisedFunctions with AuthRedirects with Logging {

  def forbiddenView(implicit request: Request[_]): Html

  protected def withAuthorisedAsClient[A](body: (String, ClientIdentifiers, Option[Utr]) => Future[Result])(
    implicit request: Request[A],
    hc: HeaderCarrier,
    ec: ExecutionContext): Future[Result] = {
    authorised(AuthProviders(GovernmentGateway))
      .retrieve(affinityGroup and allEnrolments) {
        case affinityG ~ allEnrols =>
          def clientId(serviceName: String, identifierKey: String): Option[String] =
            allEnrols.getEnrolment(serviceName).flatMap(_.getIdentifier(identifierKey).map(_.value))

          val mtdItId = clientId("HMRC-MTD-IT", "MTDITID").map(MtdItId(_))
          val nino = clientId("HMRC-NI", "NINO").map(Nino(_))
          val vrn = clientId("HMRC-MTD-VAT", "VRN").map(Vrn(_))
          val utr = clientId("HMRC-TERS-ORG", "SAUTR").map(Utr(_))
          val urn = clientId("HMRC-TERSNT-ORG", "URN").map(Urn(_))
          val cgtRef = clientId("HMRC-CGT-PD", "CGTPDRef").map(CgtRef(_))
          val pptRef = clientId("HMRC-PPT-ORG", "EtmpRegistrationNumber").map(PptRef(_))
          val cbcUkRef = clientId("HMRC-CBC-ORG", "cbcId").map(CbcId(_))
          val cbcNonUkRef = clientId("HMRC-CBC-NONUK-ORG", "cbcId").map(CbcId(_))
          val plrId = clientId("HMRC-PILLAR2-ORG", "PLRID").map(PlrId(_))

          val clientIds = ClientIdentifiers(mtdItId, nino, vrn, utr, cgtRef, urn, pptRef, cbcUkRef, cbcNonUkRef, plrId)

          val legacySaUtr = clientId("IR-SA", "UTR").map(Utr(_))

          if (clientIds.haveAtLeastOneFieldDefined) {
            affinityG match {
              case Some(Individual)   => body("personal", clientIds, legacySaUtr)
              case Some(Organisation) => body("business", clientIds, None)
              case _ =>
                logger.warn("Client logged in with wrong affinity group")
                Future.successful(Forbidden(forbiddenView))
            }
          } else {
            logger.warn("Logged in client does not have required enrolments")
            Future.successful(Forbidden(forbiddenView))
          }
      }
      .recover(handleFailure)
  }

  def handleFailure(implicit request: Request[_]): PartialFunction[Throwable, Result] = {
    case _: NoActiveSession ⇒
      Redirect(s"$signInUrl?continue_url=$continueUrl${request.uri}&origin=$appName")

    case _: UnsupportedAuthProvider ⇒
      logger.warn(s"user logged in with unsupported auth provider")
      Forbidden(forbiddenView)
  }

  private def getString(key: String): String = config.underlying.getString(key)

  private val signInUrl = getString("bas-gateway.url")
  private val continueUrl = getString("login.continue")
  private val appName = getString("appName")
}
