import sbt._
import sbt.Keys.{baseDirectory, moduleName, organization, packageOptions}
import sbt.Package.ManifestAttributes
import sbtrelease.ReleasePlugin.autoImport.ReleaseKeys.versions
import sbtrelease.ReleasePlugin.autoImport.{ReleaseStep, releaseProcess, releaseStepTask, releaseVcs, releaseVcsSign, releaseVcsSignOff}
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.Utilities.stateW
import com.typesafe.tools.mima.plugin.MimaPlugin.autoImport.{mimaPreviousArtifacts, mimaReportBinaryIssues}
import scala.sys.process.ProcessLogger

object Release {
  val previousVersion = SettingKey[String]("previousVersion", "Previous version for Mima plugin")

  val checkBinaryIncompatibilities: ReleaseStep = { st: State =>
    st.extract.getOpt(previousVersion) match {
      case None => st
      case Some(version) =>
        st.log.info(s"Starting Mima plugin to check current build with version $version")
        val newState = reapply(Seq(mimaPreviousArtifacts := Set(organization.value %% moduleName.value % version)), st)
        releaseStepTask(mimaReportBinaryIssues)(newState)
    }
  }

  val setPreviousVersion: ReleaseStep = { st: State =>
    val version = st.get(versions).getOrElse(sys.error("This release step must be after inquireVersions"))._1
    st.log.info(s"Setting previous version to '$version'")
    val file = st.extract.get(baseDirectory)
    val content = s"""import Release._
                    |ThisBuild / previousVersion := "$version"""".stripMargin
    IO.writeLines(file / "previous_version.sbt", List(content))
    st
  }

  val commitPreviousVersion: ReleaseStep = { st: State =>
    val vcs = st.extract.get(releaseVcs) getOrElse sys.error("Aborting release. Working directory is not a repository of a recognized VCS")
    val log = new ProcessLogger {
      override def err(s: => String): Unit = st.log.info(s)
      override def out(s: => String): Unit = st.log.info(s)
      override def buffer[T](f: => T): T = st.log.buffer(f)
    }
    val fileBaseDir = st.extract.get(baseDirectory)
    val file = (fileBaseDir / "previous_version.sbt").getCanonicalFile
    val base = vcs.baseDir.getCanonicalFile
    val sign = st.extract.get(releaseVcsSign)
    val signOff = st.extract.get(releaseVcsSignOff)
    val relativePath = IO.relativize(base, file) getOrElse s"Version file [$file] is outside of this VCS repository with base directory [$base]"

    vcs.add(relativePath) !! log
    val status = vcs.status.!!.trim
    if (status.nonEmpty) {
      val version = st.get(versions).getOrElse(sys.error("This release step must be after inquireVersions"))._1
      val message = s"Setting previous version to '$version'"
      st.log.info(s"Preparing commit with message: $message")
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
      checkBinaryIncompatibilities, // <- extra step
      setPreviousVersion,           // <- extra step
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
