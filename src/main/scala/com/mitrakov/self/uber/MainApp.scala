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

    //println(Calculator.distance(59.88469528, 30.35969317, 59.88633709, 30.33099622))

    val app: HttpApp[IO] = createApp
    val loggedApp: HttpApp[IO] = Logger.httpApp(logHeaders = true, logBody = true)(app)
    val server = createServer(loggedApp)

    server.compile.drain.as(ExitCode.Success)
  }

  def createApp: HttpApp[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl._
    import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
    import org.http4s.circe.CirceEntityCodec._

    val rootService = HttpRoutes.of[IO] {
      case req @ POST -> Root / "tariff" =>
        for {
          coords <- req.as[Coordinates]
          resp <- Ok(Calculator.getPrices(coords))
        } yield resp
    }

    Router("/" -> rootService).orNotFound
  }

  def createServer(app: HttpApp[IO]): Stream[IO, ExitCode] = {
    BlazeServerBuilder[IO].bindHttp(30001, "0.0.0.0").withHttpApp(app).serve
  }
}
