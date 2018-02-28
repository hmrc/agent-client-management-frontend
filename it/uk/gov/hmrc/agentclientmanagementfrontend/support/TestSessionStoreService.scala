package uk.gov.hmrc.agentclientmanagementfrontend.support

import uk.gov.hmrc.agentclientmanagementfrontend.models.ArnCache
import uk.gov.hmrc.agentclientmanagementfrontend.services.SessionStoreService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class TestSessionStoreService extends SessionStoreService(null) {

  class Session (var arnCache: Option[ArnCache] = None)

  private val sessions = collection.mutable.Map[String,Session]()

  private def sessionKey(implicit hc: HeaderCarrier): String = hc.userId match {
    case None => "default"
    case Some(userId) => userId.toString
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

  override def fetchArnCache(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ArnCache]] = {
    Future successful currentSession.arnCache
  }

  override def storeArnCache(arnCache: ArnCache)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    Future.successful(
      currentSession.arnCache = Some(arnCache)
    )

  override def remove()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    Future {
      sessions.remove(sessionKey)
    }
}
