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

import play.api.mvc.{Request, Result}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, Vrn}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrievals.authorisedEnrolments
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.agentclientmanagementfrontend.models.OptionalClientIdentifiers

import scala.concurrent.{ExecutionContext, Future}

trait AuthActions extends AuthorisedFunctions {

  protected def withAuthorisedAsAgent[A](body: Arn => Future[Result])(implicit request: Request[A], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] =
    withEnrolledFor("HMRC-AS-AGENT", "AgentReferenceNumber") {
      case Some(arn) => body(Arn(arn))
      case None => Future.failed(InsufficientEnrolments("AgentReferenceNumber identifier not found"))
    }

  protected def withAuthorisedAsClient[A](body: OptionalClientIdentifiers => Future[Result])(implicit request: Request[A], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {

    def clientId(serviceName: String, identifierKey: String)(implicit enrolments: Enrolments): Option[String] =
      enrolments.getEnrolment(serviceName).flatMap(_.getIdentifier(identifierKey).map(_.value))

    authorised(
      Enrolment("HMRC-MTD-IT") or Enrolment("HMRC-NI") or Enrolment("HMRC-MTD-VAT")
        and AuthProviders(GovernmentGateway))
      .retrieve(authorisedEnrolments) { implicit enrolments =>
        val mtdItId = clientId("HMRC-MTD-IT", "MTDITID").map(MtdItId(_))
        val nino = clientId("HMRC-NI", "NINO").map(Nino(_))
        val vrn = clientId("HMRC-MTD-VAT", "VRN").map(Vrn(_))
        val clientIds = OptionalClientIdentifiers(mtdItId, nino, vrn)

        if (clientIds.atLeastOneFieldDefined)
          body(clientIds)
        else
          Future.failed(InsufficientEnrolments("Identifiers not found"))
      }
  }

  protected def withEnrolledFor[A](serviceName: String, identifierKey: String)(body: Option[String] => Future[Result])(implicit request: Request[A], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    authorised(
      Enrolment(serviceName)
        and AuthProviders(GovernmentGateway))
      .retrieve(authorisedEnrolments) { enrolments =>
        val id = for {
          enrolment <- enrolments.getEnrolment(serviceName)
          identifier <- enrolment.getIdentifier(identifierKey)
        } yield identifier.value

        body(id)
      }
  }

}
