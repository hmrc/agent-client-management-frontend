
import play.sbt.routes.RoutesKeys

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.agentclientmanagementfrontend.views.html.main_template",
  "uk.gov.hmrc.agentclientmanagementfrontend.views.html.components._",
  "uk.gov.hmrc.govukfrontend.views.html.components._",
)

lazy val root = (project in file("."))
  .settings(
    name := "agent-client-management-frontend",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.13.10",
    majorVersion := 1,
    scalacOptions ++= Seq(
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
    ),
    PlayKeys.playDefaultPort := 9568,
    libraryDependencies ++= AppDependencies.compileDeps ++ AppDependencies.test,
    //fix for scoverage compile errors for scala 2.13.10
    libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always),
    CodeCoverageSettings.scoverageSettings,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources"
  )
  .configs(IntegrationTest)
  .settings(
    IntegrationTest / Keys.fork := true,
    Defaults.itSettings,
    IntegrationTest / unmanagedSourceDirectories += baseDirectory(_ / "it").value,
    IntegrationTest / parallelExecution := false,
    Test / parallelExecution := false
  )
  .settings(
    RoutesKeys.routesImport += "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl"
  )
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
