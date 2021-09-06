import com.typesafe.tools.mima.plugin.MimaPlugin.autoImport.{mimaPreviousArtifacts, mimaReportBinaryIssues}
import sbt.Keys._
import sbt._
import sbt.Package.ManifestAttributes
import sbtrelease.ReleasePlugin.autoImport.ReleaseKeys.versions
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.Utilities.stateW
import scala.sys.process.ProcessLogger

object Release {
  val previousVersion = SettingKey[String]("previousVersion", "Previous version string for Mima plugin")
  val previousVersionFile = SettingKey[File]("previousVersionFile", "Previous version file for Mima plugin")
  previousVersion := "0.0.0"
  previousVersionFile := baseDirectory.value / "previous_version.sbt"

  val readPreviousVersion: ReleaseStep = { st: State =>
    st.log.info(s"AAAAAAAA hey hey hey")
    val file = st.extract.get(previousVersionFile)
    if (file.exists()) {
      val version = IO.readLines(file).headOption getOrElse sys.error(s"Cannot read $file")
      st.log.info(s"Previous version found: $version")
      reapply(Seq(mimaPreviousArtifacts := Set(organization.value %% moduleName.value % version)), st)
    } else st
  }

  val checkBinaryIncompatibilities: ReleaseStep = { st: State =>
    st.extract.get(mimaPreviousArtifacts).toList match {
      case head :: _ =>
        st.log.info(s"Starting Mima plugin to check current build with version ${head.revision}")
        releaseStepTask(mimaReportBinaryIssues)(st)
      case _ => st
    }
  }

  val setPreviousVersion: ReleaseStep = { st: State =>
    val version = st.get(versions).getOrElse(sys.error("This release step must be after inquireVersions"))._1
    st.log.info(s"Setting previous version to '$version'")
    val file = st.extract.get(previousVersionFile)
    val content = s"""import Release._
                    |ThisBuild / previousVersion := "$version"""".stripMargin
    IO.writeLines(file, List(content))
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
      val version = st.get(versions).getOrElse(sys.error("This release step must be after inquireVersions"))._1
      val message = s"Setting previous version to '$version'"
      st.log.info(s"Preparing commit with message: $message")
      vcs.commit(message, sign, signOff) ! log
    }
    reapply(Seq(packageOptions += ManifestAttributes("Vcs-Release-Hash" -> vcs.currentHash)), st)
  }

  val settings: Seq[Def.Setting[_]] = Seq(
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
  )
}
