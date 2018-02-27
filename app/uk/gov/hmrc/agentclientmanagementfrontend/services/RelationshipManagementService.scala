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

import javax.inject.Inject

import uk.gov.hmrc.agentclientmanagementfrontend.connectors.{AgentServicesAccountConnector, DesConnector, PirRelationshipConnector}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class RelationshipManagementService @Inject()(pirRelationshipConnector: PirRelationshipConnector,
                                              desConnector: DesConnector,
                                              agentServicesAccountConnector: AgentServicesAccountConnector){

  def getClientPirRelationshipsArns(clientId: MtdItId)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[List[Arn]] ={
    pirRelationshipConnector.getClientRelationships(clientId).map{
      case Some(relationships) => relationships.map(rel => rel.arn)
      case None => List.empty
    }
  }

  def getClientItsaRelationshipsArns(clientId: MtdItId)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[List[Arn]] = {
    desConnector.getActiveClientItsaRelationships(clientId).map {
      case Some(relationship) => List(relationship.arn)
      case None => List.empty
    }
  }
}
