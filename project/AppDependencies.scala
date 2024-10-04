import sbt.*

object AppDependencies {

  private val mongoVersion: String = "1.9.0"
  private val bootstrapVersion: String = "8.6.0"

  lazy val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30" % "8.5.0",
    "uk.gov.hmrc"       %% "play-partials-play-30"      % "9.1.0",
    "uk.gov.hmrc"       %% "agent-mtd-identifiers"      % "2.0.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"         % mongoVersion,
    "uk.gov.hmrc"       %% "crypto-json-play-30"        % "8.1.0"
  )

  lazy val test: Seq[ModuleID] = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play"      % "6.0.1"           % Test,
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"  % bootstrapVersion  % Test,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30" % mongoVersion      % Test
  )

}
