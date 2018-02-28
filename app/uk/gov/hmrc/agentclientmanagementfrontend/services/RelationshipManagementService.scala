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

package uk.gov.hmrc.agentclientmanagementfrontend.services

import java.util.UUID
import javax.inject.Inject

import uk.gov.hmrc.agentclientmanagementfrontend.connectors.{AgentServicesAccountConnector, DesConnector, PirRelationshipConnector}
import uk.gov.hmrc.agentclientmanagementfrontend.models.{ArnCache, AuthorisedAgent}
import uk.gov.hmrc.agentmtdidentifiers.model.MtdItId
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class RelationshipManagementService @Inject()(pirRelationshipConnector: PirRelationshipConnector,
                                              desConnector: DesConnector,
                                              agentServicesAccountConnector: AgentServicesAccountConnector,
                                              sessionStoreService: SessionStoreService) {

  def getAuthorisedAgents(clientId: MtdItId)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Seq[AuthorisedAgent]] = {
    val relationshipWithAagencyNames = for {
      pir <- pirRelationshipConnector.getClientRelationships(clientId)
      itsa <- desConnector.getActiveClientItsaRelationships(clientId).map(_.toSeq)
      relationships = itsa ++ pir
      agencyNames <- agentServicesAccountConnector.getAgencyNames(relationships.map(_.arn)) if relationships.nonEmpty
    } yield (relationships, agencyNames)

    relationshipWithAagencyNames.flatMap {
      case (relationships, agencyNames) =>
        val authorisedAgents = relationships.map { relationship =>
          val uuId = UUID.randomUUID().toString.replace("-", "")
          sessionStoreService.storeArnCache(ArnCache(uuId, relationship.arn))
            .map(_ => AuthorisedAgent(uuId, relationship.serviceName, agencyNames.getOrElse(relationship.arn, "")))
        }

        Future.sequence(authorisedAgents)
    }
  }
}