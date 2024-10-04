package uk.gov.hmrc.agentclientmanagementfrontend.util

import play.api.Logging
import play.api.libs.json.{Format, Json}

import scala.util.Try

object StringFormatFallbackSetup extends Logging {

  def stringFormatFallback(format: Format[String]): Format[String] =
    Format(
      json =>
        Try(format.reads(json)).recover { case e: Throwable =>
          logger.warn(s"[StringFormatFallbackSetup][stringFormatFallback] failed to decrypt string: ${e.getMessage}")
          Json.fromJson[String](json)
        }.get,
      (value: String) => format.writes(value)
    )
}
