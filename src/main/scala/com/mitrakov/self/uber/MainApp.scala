package com.mitrakov.self.uber

import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Timer}
import com.mitrakov.self.uber.yandex.{CommonResponse, Token, YandexPay}
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.server.middleware.Logger
import fs2.Stream
import org.http4s.server.blaze.BlazeServerBuilder

object MainApp extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val app: HttpApp[IO] = createApp
    val loggedApp: HttpApp[IO] = Logger.httpApp(logHeaders = true, logBody = true)(app)
    val server = createServer(loggedApp)

    server.compile.drain.as(ExitCode.Success)
  }

  def createApp[F[_]: Concurrent: ContextShift]: HttpApp[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
    import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
    import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
    import cats.implicits.toFlatMapOps
    import cats.implicits.toFunctorOps

    val rootService = HttpRoutes.of[F] {
      case GET -> Root / "health" => Ok("ok")
      case GET -> Root / "version" => Ok("1.0.3")
      case req @ GET -> Root / "headers" => Ok(req.headers.toList.map(h => s"${h.name}:${h.value}").mkString("Headers: ",",",""))
      case req @ POST -> Root / "tariff" =>
        for {
          coords <- req.as[Coordinates]
          resp <- Ok(Calculator.getPrices(coords))
        } yield resp
      case req @ POST -> Root / "payment" =>
        for {
          token <- req.as[Token]
          result <- YandexPay.sendPayment(token)
          resp <- result.body match {
            case Right(paymentResponse) => Ok(CommonResponse(paymentResponse.confirmation.flatMap(_.confirmation_url).getOrElse("")))
            case Left(err) => sys.error(err.getMessage); BadRequest(CommonResponse("", 1, err.getMessage))
          }
        } yield resp
    }

    Router("/" -> rootService).orNotFound
  }

  def createServer[F[_]: ConcurrentEffect: Timer](app: HttpApp[F]): Stream[F, ExitCode] = {
    BlazeServerBuilder[F].bindHttp(8080, "0.0.0.0").withHttpApp(app).serve
  }
}
