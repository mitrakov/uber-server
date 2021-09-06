import sbt.Package.ManifestAttributes
import sbtrelease.ReleasePlugin.autoImport.ReleaseKeys.versions
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.Utilities.stateW
import sbtrelease.Vcs

import scala.sys.process.ProcessLogger

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
lazy val setPreviousVersion: ReleaseStep = { st: State =>
  //val globalVersionString = """ThisBuild / version := "%s""""
  //val versionString = """version := "%s""""
  //val versions = AttributeKey[Versions]("releaseVersions")
  val vs: (String, String) = st.get(versions).getOrElse(sys.error("No versions are set! Was this release part executed before inquireVersions?")) // 1.0.6 -> 1.0.7-SNAPSHOT
  //val currentSnapshot = selectVersion(vs)
  val current = vs._1

  //val useGlobal = st.extract.get(releaseUseGlobalVersion)
  //val curreentSnapshot: String = st.extract.get(if (useGlobal) ThisBuild / version else version)
  //val current: String = Version(currentSnapshot) map (_.withoutQualifier.string) getOrElse versionFormatError(currentSnapshot)
  st.log.info("Setting OLD version to '%s'." format current)
  //val versionStr = (if (useGlobal) globalVersionString else versionString) format current
  //val file = st.extract.get(releaseVersionFile)
  //IO.writeLines(file, Seq(versionStr))
  val baseDir = st.extract.get(baseDirectory)
  val file = baseDir / "previous_version"
  IO.writeLines(file, List(current))

  //val hhh = st.extract.(ReleasePlugin.runtimeVersion)
  //reapply(Seq(if (useGlobal) ThisBuild / version := current else version := current), st)
  st
}

// === commit
def toProcessLogger(st: State): ProcessLogger = new ProcessLogger {
  override def err(s: => String): Unit = st.log.info(s)
  override def out(s: => String): Unit = st.log.info(s)
  override def buffer[T](f: => T): T = st.log.buffer(f)
}
def vcs(st: State): Vcs = {
  st.extract.get(releaseVcs).getOrElse(sys.error("Aborting release. Working directory is not a repository of a recognized VCS."))
}

lazy val commitPreviousVersion = ReleaseStep({ st: State =>
  val newState = commitVersion(st, releaseCommitMessage) // TODO
  reapply(Seq[Setting[_]](
    packageOptions += ManifestAttributes("Vcs-Release-Hash" -> vcs(st).currentHash)
  ), newState)
}, identity)

//lazy val commitNextVersion = {st: State => commitVersion(st, releaseNextCommitMessage)}

def commitVersion: (State, TaskKey[String]) => State = { (st: State, commitMessage: TaskKey[String]) =>
  val log = toProcessLogger(st)
  val baseDir = st.extract.get(baseDirectory)
  val file = (baseDir / "previous_version").getCanonicalFile
  //val file = st.extract.get(releaseVersionFile).getCanonicalFile
  val base = vcs(st).baseDir.getCanonicalFile
  val sign = st.extract.get(releaseVcsSign)
  val signOff = st.extract.get(releaseVcsSignOff)
  val relativePath = IO.relativize(base, file).getOrElse("Version file [%s] is outside of this VCS repository with base directory [%s]!" format(file, base))

  vcs(st).add(relativePath) !! log
  val status = vcs(st).status.!!.trim

  val newState = if (status.nonEmpty) {
    val (state, msg) = st.extract.runTask(commitMessage, st)
    vcs(state).commit(msg, sign, signOff) ! log
    state
  } else st // nothing to commit. this happens if the version.sbt file hasn't changed.
  newState
}
// === eof commit

// read version
val previousVersion = SettingKey[String]("previousVersion", "Prev version")
previousVersion := "0.0.0"
def readPreviousVersion: ReleaseStep = { st: State =>
  val baseDir = st.extract.get(baseDirectory)
  val file = baseDir / "previous_version"
  if (file.exists()) {
    val v = IO.readLines(file).headOption getOrElse "0.0.0"
    reapply(Seq(previousVersion := v), st)
  } else st
}
// === eof read v

// check file
def checkBinaryIncompatibilities: ReleaseStep = { st: State =>
  st.extract.get(previousVersion) match {
    case "0.0.0" => releaseStepTask(mimaReportBinaryIssues)(st)
    case _ => st
  }
}
// === eof check file

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,              // : ReleaseStep
  inquireVersions,                        // : ReleaseStep
  runClean,                               // : ReleaseStep
  runTest,                                // : ReleaseStep
  readPreviousVersion,
  checkBinaryIncompatibilities,
  setPreviousVersion,
  commitPreviousVersion,
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
//def previousVersion(v: String): String = Version(v) map (_.withoutQualifier.string) getOrElse versionFormatError(v)
mimaPreviousArtifacts := Set(organization.value %% moduleName.value % previousVersion.value)
