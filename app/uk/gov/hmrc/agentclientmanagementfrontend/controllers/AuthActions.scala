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

import play.api.mvc.{Request, Result}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, Utr, Vrn}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.agentclientmanagementfrontend.models.OptionalClientIdentifiers

import scala.concurrent.{ExecutionContext, Future}

trait AuthActions extends AuthorisedFunctions {

  protected def withAuthorisedAsClient[A](body: (String, OptionalClientIdentifiers) => Future[Result])(implicit request: Request[A], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {

    def clientId(serviceName: String, identifierKey: String)(implicit enrolments: Enrolments): Option[String] =
      enrolments.getEnrolment(serviceName).flatMap(_.getIdentifier(identifierKey).map(_.value))

    authorised(AuthProviders(GovernmentGateway))
      .retrieve(affinityGroup and allEnrolments) {
        case affinityG ~ allEnrols =>

          val determineAffinityGroup = (affinity: AffinityGroup) => affinity match {
            case AffinityGroup.Individual => "personal"
            case AffinityGroup.Organisation => "business"
            case _ => throw new IllegalStateException(s"Unsupported Affinity Group: $affinity")
          }

          implicit val enrolments: Enrolments = allEnrols
          val mtdItId = clientId("HMRC-MTD-IT", "MTDITID").map(MtdItId(_))
          val nino = clientId("HMRC-NI", "NINO").map(Nino(_))
          val vrn = clientId("HMRC-MTD-VAT", "VRN").map(Vrn(_))
          val utr = clientId("HMRC-TERS-ORG", "SAUTR").map(Utr(_))
          val clientIds = OptionalClientIdentifiers(mtdItId, nino, vrn, utr)

          (affinityG, clientIds) match {
          case (Some(a), ids) if AffinityGroup.Individual == a && ids.haveAtLeastOneFieldDefined => body(determineAffinityGroup(a), clientIds)
          case (Some(a), ids) if AffinityGroup.Organisation == a && ids.haveAtLeastOneFieldDefined => body(determineAffinityGroup(a), clientIds)
          case _ => Future.failed(InsufficientEnrolments("Identifiers not found"))
        }

      }
  }
}
