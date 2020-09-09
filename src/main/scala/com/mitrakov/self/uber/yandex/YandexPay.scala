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

/* Response example: {
  "id" : "26e5e436-000f-5000-8000-1d3beb3d0eb5",
  "status" : "pending",
  "paid" : false,
  "amount" : {
    "value" : "1.00",
    "currency" : "RUB"
  },
  "confirmation" : {
    "type" : "redirect",
    "confirmation_url" : "https://3ds-gate.yamoney.ru/card-auth?acsUri=https%3A%2F%2Fds1.mirconnect.ru%3A443%2Fvbv1%2Fpareq&MD=YTJlNzQ5YTBhMGVmZWExMWI0NmJlYmRkYjg3ZGU2YTU6MTc0MjU0MDgy&PaReq=eJxVUkFuwjAQPPMLxLESsWMnJCBjiRap5RAaUXrgaMwWgkgIdtLC7%2Bs1UNGDpZ0Ze%2B2ZtVjuDMD0A3RrQIoMrFVb6BabcY9zzlQSpn2VQNKPIpX0FU3Xfc0dOfxa0yRlPdkR%2BWQBJ9npiG8wtjhWMgxowAS5Q6dkYPROVY2rO0Lp0%2FNsLiPGYp4KcoOolGBmU8nCAU3iyHW4YlQqVYJcZU%2B5ury60griGZT0sa0ac5GDiAtyByi05iB3TVPbESFlfWi3QdkemkIr06jAtCTLZ2QBtj5WFnJz1M56UW2Dva0FwbOuBXl4uMhbrO311nOxkdnyM35fZpf3acbny9V5vp%2F8ZFO%2FxoLgDty5UQ1IRhmlQxp3w2QUs1EcC%2BJ5n0eJj8bUqEvjCpCv8b7JTUTtkfDOW2Og0nfrd4QSnJ0tcPtcin81GnowIV7ebvPQjYs5HvI0iWgU4kg8c2tUuDwZp6HvVPhwBcGzrpsfPcbkv42r%2Fn2nX7U4tko%3D&TermUrl=https%3A%2F%2Fcheckout.gateline.net%2Fsecure3d%2Frf3d"
  },
  "created_at" : "2020-09-05T17:52:54.390Z",
  "description" : "Item description",
  "metadata" : {
    "scid" : "1880592"
  },
  "payment_method" : {
    "type" : "bank_card",
    "id" : "26e5e436-000f-5000-8000-1d3beb3d0eb5",
    "saved" : false,
    "card" : {
      "first6" : "437773",
      "last4" : "6594",
      "expiry_month" : "01",
      "expiry_year" : "2023",
      "card_type" : "Visa",
      "issuer_country" : "RU",
      "issuer_name" : "Tcs Bank (Cjsc)"
    },
    "title" : "Bank card *6594"
  },
  "recipient" : {
    "account_id" : "733743",
    "gateway_id" : "1751332"
  },
  "refundable" : false,
  "test" : false
}*/
