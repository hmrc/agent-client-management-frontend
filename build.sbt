import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := """uk\.gov\.hmrc\.BuildInfo;.*\.Routes;.*\.RoutesPrefix;.*Filters?;MicroserviceAuditConnector;Module;GraphiteStartUp;.*\.Reverse[^.]*""",
    ScoverageKeys.coverageMinimum := 80.00,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution := false
  )
}

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.agentclientmanagementfrontend.views.html.main_template",
  "uk.gov.hmrc.agentclientmanagementfrontend.views.html.components._",
  "uk.gov.hmrc.govukfrontend.views.html.components._",
)

lazy val compileDeps = Seq(
  "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % "5.20.0",
  "uk.gov.hmrc" %% "play-frontend-hmrc"         % "3.5.0-play-28",
  "uk.gov.hmrc" %% "play-partials"              % "8.3.0-play-28",
  "uk.gov.hmrc" %% "agent-kenshoo-monitoring"   % "4.8.0-play-28",
  "uk.gov.hmrc" %% "agent-mtd-identifiers"      % "0.34.0-play-28",
  "uk.gov.hmrc" %% "mongo-caching"              % "7.1.0-play-28"
)

def testDeps(scope: String) = Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % scope,
  "org.scalatestplus" %% "mockito-3-12" % "3.2.10.0" % scope,
  "org.jsoup" % "jsoup" % "1.14.2" % scope,
  "uk.gov.hmrc" %% "reactivemongo-test" % "5.0.0-play-27" % scope,
  "com.github.tomakehurst" % "wiremock-jre8" % "2.26.2" % scope,
  "com.vladsch.flexmark" %  "flexmark-all" % "0.35.10" % scope
)

lazy val root = (project in file("."))
  .settings(
    name := "agent-client-management-frontend",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.12.12",
    majorVersion := 0,
    scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-Xlint:-missing-interpolator,_",
      "-Yno-adapted-args",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:implicitConversions",
      "-P:silencer:pathFilters=views;routes;TestStorage"),
    PlayKeys.playDefaultPort := 9568,
    resolvers := Seq(
      Resolver.typesafeRepo("releases"),
    ),
    resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2",
    resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns),
    resolvers += "HMRC-local-artefacts-maven" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases-local",

libraryDependencies ++= compileDeps ++ testDeps("test") ++ testDeps("it"),
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.0" cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % "1.7.0" % Provided cross CrossVersion.full
    ),
    publishingSettings,
    scoverageSettings,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources"
  )
  .configs(IntegrationTest)
  .settings(
    IntegrationTest / Keys.fork := true,
    Defaults.itSettings,
    IntegrationTest / unmanagedSourceDirectories += baseDirectory(_ / "it").value,
    IntegrationTest / parallelExecution := false
  )
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
