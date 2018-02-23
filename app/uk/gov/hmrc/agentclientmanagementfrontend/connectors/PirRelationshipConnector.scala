package uk.gov.hmrc.agentclientmanagementfrontend.connectors

import java.net.URL
import javax.inject.{Inject, Named, Singleton}

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentclientmanagementfrontend.models.PirRelationship
import uk.gov.hmrc.agentclientmanagementfrontend.util.Services
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PirRelationshipConnector @Inject()(
                                          @Named("agent-fi-relationship-baseUrl") baseUrl: URL,
                                          http: HttpGet with HttpPost with HttpPut with HttpDelete,
                                          metrics: Metrics) extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  private def craftUrl(location: String) = new URL(baseUrl, location)

  private def pirClientIdUrl(clientId: String): String =
    s"/agent-fi-relationship/relationships/service/${Services.HMRCPIR}/clientId/$clientId"

  def getClientRelationships(clientId: String)(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[List[PirRelationship]]] = {
    getRelationshipList(pirClientIdUrl(clientId))
  }

  def getRelationshipList(location: String)(implicit hc: HeaderCarrier): Future[Option[List[PirRelationship]]] = {
    monitor(s"ConsumedAPI-Get-AfiRelationship-GET") {
      val url = craftUrl(location)
      http.GET[Option[List[PirRelationship]]](url.toString)
    }
  }
}
