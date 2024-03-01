import sbt.Compile
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion        := "2.13.12"

lazy val microservice = Project("excise-movement-control-system-api", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin, ScalaxbPlugin)
  .settings(
    PlayKeys.playDefaultPort := 10250,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    dependencyOverrides ++= AppDependencies.overrides,
    // https://www.scala-lang.org/2021/01/12/configuring-and-suppressing-warnings.html
    // suppress warnings in generated routes files
    scalacOptions += "-Wconf:src=routes/.*:s,src=src_managed/.*:s",
      // scalaxb
    Compile / scalaxb / scalaxbDispatchVersion := AppDependencies.dispatchVersion,
    Compile / scalaxb / scalaxbPackageName := "generated",
  )
  .settings(
    Test / parallelExecution := true
  )
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(scoverageSettings: _*)
  .settings(
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
  )
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427


lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())

lazy val scoverageSettings: Seq[Setting[_]] = Seq(
  ScoverageKeys.coverageExcludedPackages := List("<empty>",
    "Reverse.*",
    "domain\\..*",
    "models\\..*",
    "metrics\\..*",
    ".*(BuildInfo|Routes|Options).*",
    "generated\\..*",
    "scalaxb\\..*"
  ).mkString(";"),
  ScoverageKeys.coverageMinimumStmtTotal := 90,
  ScoverageKeys.coverageFailOnMinimum := true,
  ScoverageKeys.coverageHighlighting := true
)
