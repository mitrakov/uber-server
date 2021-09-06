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
