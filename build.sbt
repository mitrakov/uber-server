import sbtrelease.ReleaseStateTransformations._
import sbtrelease.Utilities.stateW
import sbtrelease.{Version, versionFormatError}

name := "uber-server"
organization := "com.mitrakov.self"
scalaVersion := "2.13.1"

val http4sVersion = "0.21.1"
val sttpVersion = "2.2.7"

libraryDependencies ++= Seq(
  "org.http4s"    %% "http4s-blaze-server" % http4sVersion,
  "org.http4s"    %% "http4s-dsl"          % http4sVersion,
  "org.http4s"    %% "http4s-circe"        % http4sVersion,
  "com.softwaremill.sttp.client" %% "core" % sttpVersion,
  "com.softwaremill.sttp.client" %% "circe" % sttpVersion,
  "com.softwaremill.sttp.client" %% "async-http-client-backend-cats" % sttpVersion,
  "io.circe"      %% "circe-generic"       % "0.13.0",
  "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime,
  "org.scalatest" %% "scalatest" % "3.2.0" % Test,
)

// sbt-assembly plugin settings
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", _*) => MergeStrategy.discard
  case "application.conf"       => MergeStrategy.concat
  case "reference.conf"         => MergeStrategy.concat
  case _                        => MergeStrategy.first
}

// publish settings
val repository = "Trix" at "https://mymavenrepo.com/repo/81Ab7uIF2XWySZknUPdN/"
publishTo in ThisBuild := Some(repository)

// release plugin
//lazy val setReleaseVersion: ReleaseStep = setVersion(_._1)
//lazy val setNextVersion: ReleaseStep = setVersion(_._2)
lazy val setOldVersion: ReleaseStep = { st: State =>
  val globalVersionString = """ThisBuild / version := "%s""""
  val versionString = """version := "%s""""
  //val vs: (String, String) = st.get(versions).getOrElse(sys.error("No versions are set! Was this release part executed before inquireVersions?")) // 1.0.6 -> 1.0.7-SNAPSHOT
  //val selected = selectVersion(vs)

  val useGlobal = st.extract.get(releaseUseGlobalVersion)
  val selected = st.extract.get(if (useGlobal) ThisBuild / version else version)
  st.log.info("Setting OLD version to '%s'." format selected)
  val versionStr = (if (useGlobal) globalVersionString else versionString) format selected
  //val file = st.extract.get(releaseVersionFile)
  //IO.writeLines(file, Seq(versionStr))
  val baseDir = st.extract.get(baseDirectory)
  val file = baseDir / "previous_version.sbt"
  IO.writeLines(file, List(versionStr))

  //reapply(Seq(if (useGlobal) ThisBuild / version := selected else version := selected), st)
  st
}



releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,              // : ReleaseStep
  inquireVersions,                        // : ReleaseStep
  runClean,                               // : ReleaseStep
  runTest,                                // : ReleaseStep
  setOldVersion,
  releaseStepTask(mimaReportBinaryIssues),
  setReleaseVersion,                      // : ReleaseStep
  commitReleaseVersion,                   // : ReleaseStep, performs the initial git checks
  tagRelease,                             // : ReleaseStep
  publishArtifacts,                       // : ReleaseStep, checks whether `publishTo` is properly set up
  setNextVersion,                         // : ReleaseStep
  commitNextVersion,                      // : ReleaseStep
  pushChanges                             // : ReleaseStep, also checks that an upstream branch is properly configured
)

// mima plugin
resolvers += repository
def previousVersion(v: String): String = Version(v) map (_.withoutQualifier.string) getOrElse versionFormatError(v)
mimaPreviousArtifacts := Set(organization.value %% moduleName.value % previousVersion(version.value))
