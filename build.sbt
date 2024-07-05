import uk.gov.hmrc.{DefaultBuildSettings, SbtAutoBuildPlugin}
import CodeCoverageSettings.scoverageSettings
import play.sbt.routes.RoutesKeys

val appName = "agent-client-management-frontend"

ThisBuild / majorVersion := 1
ThisBuild / scalaVersion := "2.13.12"

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.agentclientmanagementfrontend.views.html.main_template",
  "uk.gov.hmrc.agentclientmanagementfrontend.views.html.components._",
  "uk.gov.hmrc.govukfrontend.views.html.components._",
)


val scalaCOptions = Seq(
  "-Werror",
  "-Wdead-code",
  "-feature",
  "-language:implicitConversions",
  "-Xlint",
  "-Wconf:src=target/.*:s", // silence warnings from compiled files
  "-Wconf:src=*html:w", // silence html warnings as they are wrong
  "-Wconf:cat=deprecation:s",
  "-Wconf:cat=unused-privates:s",
  "-Wconf:msg=match may not be exhaustive:is", // summarize warnings about non-exhaustive pattern matching
)


lazy val root = (project in file("."))
  .settings(
    name := appName,
    organization := "uk.gov.hmrc",
    PlayKeys.playDefaultPort := 9568,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    resolvers ++= Seq(Resolver.typesafeRepo("releases")),
    scalacOptions ++= scalaCOptions,
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources"
  )
  .settings(
    RoutesKeys.routesImport += "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl"
  )
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(
    Test / parallelExecution := false,
    scoverageSettings
  )
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)


lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(root % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.test)
  .settings(
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true
  )

