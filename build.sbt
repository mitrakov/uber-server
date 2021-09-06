import sbt.Package.ManifestAttributes
import sbtrelease.ReleasePlugin.autoImport.ReleaseKeys.versions
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.Utilities.stateW

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
resolvers += repository
ThisBuild / publishTo := Some(repository)

// release plugin
val ZeroVersion = "0.0.0"
val previousVersion = SettingKey[String]("previousVersion", "Previous version string for Mima plugin")
val previousVersionFile = SettingKey[File]("previousVersionFile", "Previous version file for Mima plugin")
previousVersion := ZeroVersion
previousVersionFile := baseDirectory.value / "previous_version"

val readPreviousVersion: ReleaseStep = { st: State =>
  val file = st.extract.get(previousVersionFile)
  if (file.exists()) {
    val version = IO.readLines(file).headOption getOrElse ZeroVersion
    st.log.info(s"Previous version found: $version")
    reapply(Seq(previousVersion := version), st)
  } else st
}

val checkBinaryIncompatibilities: ReleaseStep = { st: State =>
  st.extract.get(previousVersion) match {
    case ZeroVersion => st
    case version =>
      st.log.info(s"Starting Mima plugin to check current build with version $version")
      mimaPreviousArtifacts := Set(organization.value %% moduleName.value % version)
      releaseStepTask(mimaReportBinaryIssues)(st)
  }
}

val setPreviousVersion: ReleaseStep = { st: State =>
  val vs: (String, String) = st.get(versions) getOrElse sys.error("This release step must be after inquireVersions")
  val current = vs._1

  st.log.info(s"Setting previous version to '$current'")
  val file = st.extract.get(previousVersionFile)
  IO.writeLines(file, List(current))
  st
}

val commitPreviousVersion: ReleaseStep = { st: State =>
  val vcs = st.extract.get(releaseVcs) getOrElse sys.error("Aborting release. Working directory is not a repository of a recognized VCS")
  val log = new ProcessLogger {
    override def err(s: => String): Unit = st.log.info(s)
    override def out(s: => String): Unit = st.log.info(s)
    override def buffer[T](f: => T): T = st.log.buffer(f)
  }
  val file = st.extract.get(previousVersionFile).getCanonicalFile
  val base = vcs.baseDir.getCanonicalFile
  val sign = st.extract.get(releaseVcsSign)
  val signOff = st.extract.get(releaseVcsSignOff)
  val relativePath = IO.relativize(base, file).getOrElse(s"Version file [$file] is outside of this VCS repository with base directory [$base]")

  vcs.add(relativePath) !! log
  val status = vcs.status.!!.trim
  if (status.nonEmpty) {
    val prevVersion = st.extract.get(previousVersion)
    val message = s"Setting previous version to $prevVersion"
    st.log.info(s"Preparing commit with message: $message")
    vcs.commit(message, sign, signOff) ! log
  }
  reapply(Seq(packageOptions += ManifestAttributes("Vcs-Release-Hash" -> vcs.currentHash)), st)
}

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
