import sbt._

object AppDependencies {

  private val bootstrapVersion = "7.19.0"
  val dispatchVersion        = "1.2.0"
  lazy val mongoVersion = "1.3.0"


  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-work-item-repo-play-28" % mongoVersion,
    // required for scalaxb
    "org.scala-lang.modules"  %% "scala-xml"                  % "1.3.0",
    "org.scala-lang.modules"  %% "scala-parser-combinators"   % "1.1.2",
    "javax.xml.bind"          % "jaxb-api"                    % "2.3.1",
    "org.dispatchhttp"        %% "dispatch-core"              % dispatchVersion,
    "com.beachape"            %% "enumeratum-play-json"       % "1.6.0",
    "org.typelevel"           %% "cats-core"                  % "2.10.0",
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"    % mongoVersion,
    "org.scalatestplus"       %% "scalatestplus-mockito"      % "1.0.0-M2",
    "org.mockito"             %% "mockito-scala"              % "1.17.12"
  ).map(_ % "test, it")
}
