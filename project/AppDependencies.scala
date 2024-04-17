import sbt._

object AppDependencies {

  private val bootstrapVersion = "8.4.0"
  val dispatchVersion        = "1.2.0"
  lazy val mongoVersion = "1.7.0"


  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-work-item-repo-play-30" % mongoVersion,
    "javax.xml.bind"          % "jaxb-api"                    % "2.3.1",
    "com.beachape"            %% "enumeratum-play-json"       % "1.8.0",
    "org.typelevel"           %% "cats-core"                  % "2.10.0"

  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"    % mongoVersion,
    "org.scalatestplus"       %% "scalatestplus-mockito"      % "1.0.0-M2",
    "org.mockito"             %% "mockito-scala"              % "1.17.12"
  ).map(_ % "test")

  val overrides: Seq[ModuleID] = Seq(
    "org.scala-lang.modules" % "scala-xml_2.13" % "2.2.0",
  )
}
