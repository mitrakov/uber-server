name := "uber-server"
organization := "com.mitrakov.self"
version := "1.0.0"
scalaVersion := "2.13.1"

val http4sVersion = "0.21.1"

libraryDependencies ++= Seq(
  "org.http4s"    %% "http4s-blaze-server" % http4sVersion,
  "org.http4s"    %% "http4s-dsl"          % http4sVersion,
  "org.http4s"    %% "http4s-circe"        % http4sVersion,
  "io.circe"      %% "circe-generic"       % "0.13.0",
  "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime,
  "org.scalatest" %% "scalatest" % "3.2.0" % Test,
)
