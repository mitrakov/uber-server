package com.mitrakov.self.uber

import java.util.UUID
import cats.effect.{Concurrent, ContextShift}
import sttp.client.{UriContext, Response, basicRequest}
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend
import scala.language.higherKinds

object YandexPay {
  def sendPayment[F[_]: Concurrent: ContextShift](yaToken: YaToken): F[Response[Either[String, String]]] = {
    import sttp.client.circe.circeBodySerializer
    import cats.implicits.toFlatMapOps
    import cats.implicits.catsSyntaxApplicativeId
    import cats.implicits.catsSyntaxEitherId

    sys.env get "MOBILE_SDK_KEY" match {
      case None => Response.ok("Please define MOBILE_SDK_KEY".asLeft[String]).pure
      case Some(key) =>
        AsyncHttpClientCatsBackend[F]() flatMap { implicit backend =>
          val uuid = UUID.randomUUID().toString
          val request = basicRequest
            .post(uri"https://payment.yandex.net/api/v3/payments")
            .auth.basic("733743", key)
            .headers(Map("Idempotence-Key" -> uuid))
            .body(yaToken.toPayment())
          request.send().flatTap(resp => println(resp.body).pure)
        }
    }
  }
}
