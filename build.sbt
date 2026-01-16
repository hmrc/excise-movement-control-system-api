import sbt.Compile
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "2.13.16"

lazy val generatedV1 = (project in file("generated-v1"))
  .enablePlugins(ScalaxbPlugin)
  .settings(
    scalaVersion := "2.13.16",
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "2.2.0",
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
      "javax.xml.bind" % "jaxb-api" % "2.3.1",


    ),
    Compile / scalaxb / scalaxbXsdSource := new File("app/xsd/v1"),
    Compile / scalaxb / scalaxbDispatchVersion := "1.1.3",
    Compile / scalaxb / scalaxbGenerateRuntime := true,
    Compile / scalaxb / scalaxbPackageName := "generated.v1",


  )

lazy val generatedV2 = (project in file("generated-v2"))
  .enablePlugins(ScalaxbPlugin)
  .dependsOn(generatedV1)
  .settings(
    scalaVersion := "2.13.16",
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "2.2.0",
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
      "javax.xml.bind" % "jaxb-api" % "2.3.1",


    ),
    Compile / scalaxb / scalaxbXsdSource := new File("app/xsd/v2"),
    Compile / scalaxb / scalaxbDispatchVersion := "1.1.3",
    Compile / scalaxb / scalaxbGenerateRuntime := true,
    Compile / scalaxb / scalaxbPackageName := "generated.v2",
  )

lazy val microservice = Project("excise-movement-control-system-api", file("."))
  .dependsOn(generatedV1,generatedV2)
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    PlayKeys.playDefaultPort := 10250,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    dependencyOverrides ++= AppDependencies.overrides,
    // https://www.scala-lang.org/2021/01/12/configuring-and-suppressing-warnings.html
    // suppress warnings in generated routes files
    scalacOptions += "-Wconf:src=routes/.*:s,src=src_managed/.*:s",
  )
  .settings(
    Test / parallelExecution := true
  )
  .settings(scoverageSettings *)
  .settings(scalafmtOnCompile := true)
  .settings(
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources"
  )
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())

lazy val scoverageSettings: Seq[Setting[?]] = Seq(
  ScoverageKeys.coverageExcludedPackages := List(
    "<empty>",
    "Reverse.*",
    "domain\\..*",
    "models\\..*",
    "models.auditing\\..*",
    "models.eis\\..*",
    "metrics\\..*",
    ".*(BuildInfo|Routes|Options).*",
    "generated\\..*",
    "scalaxb\\..*"
  ).mkString(";"),
  ScoverageKeys.coverageMinimumStmtTotal := 70,
  ScoverageKeys.coverageFailOnMinimum := true,
  ScoverageKeys.coverageHighlighting := true
)

addCommandAlias("runAllChecks", ";clean;compile;scalafmtAll;test;it/test")
