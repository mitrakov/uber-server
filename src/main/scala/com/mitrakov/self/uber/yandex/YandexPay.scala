package com.mitrakov.self.uber.yandex

import java.util.UUID
import cats.effect.{Concurrent, ContextShift}
import io.circe
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client.{HttpError, Response, ResponseError, basicRequest, UriContext}
import sttp.model.StatusCode

object YandexPay {
  final val SHOP_ID = "733743"

  def sendPayment[F[_]: Concurrent: ContextShift](token: Token): F[Response[Either[ResponseError[circe.Error], PaymentResponse]]] = {
    import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxEitherId, toFlatMapOps}
    import sttp.client.circe.circeBodySerializer
    import sttp.client.circe.asJson

    sys.env get "SECRET_KEY" match {
      case None =>
        Response.ok((HttpError("Please define SECRET_KEY", StatusCode.Ok): ResponseError[circe.Error]).asLeft[PaymentResponse]).pure
      case Some(key) =>
        AsyncHttpClientCatsBackend[F]() flatMap { implicit backend =>
          val uuid = UUID.randomUUID().toString
          val request = basicRequest
            .post(uri"https://payment.yandex.net/api/v3/payments")
            .auth.basic(SHOP_ID, key)
            .headers(Map("Idempotence-Key" -> uuid))
            .body(token.toPayment())
            .response(asJson[PaymentResponse])
          request.send().flatTap(resp => println(resp.body).pure)
        }
    }
  }
}
