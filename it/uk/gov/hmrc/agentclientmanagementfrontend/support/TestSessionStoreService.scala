package uk.gov.hmrc.agentclientmanagementfrontend.support

import uk.gov.hmrc.agentclientmanagementfrontend.models.ClientCache
import uk.gov.hmrc.agentclientmanagementfrontend.services.SessionStoreService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class TestSessionStoreService extends SessionStoreService(null) {

  class Session (var clientCache: Option[Seq[ClientCache]] = None)

  private val sessions = collection.mutable.Map[String,Session]()

  private def sessionKey(implicit hc: HeaderCarrier): String = hc.gaUserId match {
    case None => "default"
    case Some(userId) => userId
  }

  def currentSession(implicit hc: HeaderCarrier): Session = {
    sessions.getOrElseUpdate(sessionKey, new Session())
  }

  def clear():Unit = {
    sessions.clear()
  }

  def allSessionsRemoved: Boolean = {
    sessions.isEmpty
  }

  override def fetchClientCache(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Seq[ClientCache]]] = {
    Future successful currentSession.clientCache
  }

  override def storeClientCache(cache: Seq[ClientCache])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    Future.successful(
      currentSession.clientCache = Some(cache)
    )

  override def remove()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    Future {
      sessions.remove(sessionKey).foreach(_ => ())
    }
}
