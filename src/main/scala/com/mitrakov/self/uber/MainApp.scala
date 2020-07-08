package com.mitrakov.self.uber

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.server.middleware.Logger
import fs2.Stream
import org.http4s.server.blaze.BlazeServerBuilder

object MainApp extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    import cats.syntax.functor.toFunctorOps

    val app: HttpApp[IO] = createApp
    val loggedApp: HttpApp[IO] = Logger.httpApp(logHeaders = true, logBody = true)(app)
    val server = createServer(loggedApp)

    server.compile.drain.as(ExitCode.Success)
  }

  def createApp: HttpApp[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl._
    import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT

    val rootService = HttpRoutes.of[IO] {
      case GET -> Root / "hello" => Ok(s"ok")
    }

    Router("/" -> rootService).orNotFound
  }

  def createServer(app: HttpApp[IO]): Stream[IO, ExitCode] = {
    BlazeServerBuilder[IO].bindHttp(30001, "0.0.0.0").withHttpApp(app).serve
  }
}
