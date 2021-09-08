import sbt._
import sbt.Keys.{baseDirectory, packageOptions}
import sbt.Package.ManifestAttributes
import sbtrelease.ReleasePlugin.autoImport.ReleaseKeys.versions
import sbtrelease.ReleasePlugin.autoImport.{ReleaseStep, releaseProcess, releaseVcs, releaseVcsSign, releaseVcsSignOff}
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.Utilities.stateW
import scala.sys.process.ProcessLogger

object Release {
  val previousVersion = SettingKey[String]("previousVersion", "Previous version for Mima plugin")

  val commitPreviousVersion: ReleaseStep = { st: State =>
    val log = new ProcessLogger {
      override def err(s: => String): Unit = st.log.info(s)
      override def out(s: => String): Unit = st.log.info(s)
      override def buffer[T](f: => T): T = st.log.buffer(f)
    }
    val version = st.get(versions).getOrElse(sys.error("This release step must be after inquireVersions"))._1
    val message = s"Setting previous version to '$version'"
    st.log.info(message)

    val vcs = st.extract.get(releaseVcs) getOrElse sys.error("Aborting release. Working directory is not a repository of a recognized VCS")
    val fileBaseDir = st.extract.get(baseDirectory)
    val file = (fileBaseDir / "previous_version.sbt").getCanonicalFile
    val base = vcs.baseDir.getCanonicalFile
    val sign = st.extract.get(releaseVcsSign)
    val signOff = st.extract.get(releaseVcsSignOff)
    val relativePath = IO.relativize(base, file) getOrElse s"Version file [$file] is outside of this VCS repository with base directory [$base]"

    IO.writeLines(file, List(s"""ThisBuild / Release.previousVersion := "$version""""))

    vcs.add(relativePath) !! log
    val status = vcs.status.!!.trim
    if (status.nonEmpty) {
      vcs.commit(message, sign, signOff) ! log
    }
    reapply(Seq(packageOptions += ManifestAttributes("Vcs-Release-Hash" -> vcs.currentHash)), st)
  }

  val settings: Seq[Def.Setting[_]] = Seq(
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      commitPreviousVersion,        // <- extra step
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      publishArtifacts,
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  )
}
