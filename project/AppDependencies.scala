import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  private val bootstrapVersion = "7.19.0"
  

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % bootstrapVersion
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % bootstrapVersion,
    "org.scalatestplus"       %% "scalatestplus-mockito"      % "1.0.0-M2",
    "org.scalatestplus.play"  %% "scalatestplus-play"         % "5.1.0"
    "org.mockito"             %% "mockito-scala"              % "1.17.12",
  ).map(_ % "test, it")
}
