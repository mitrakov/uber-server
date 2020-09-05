package com.mitrakov.self

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec

package object uber {
  case class Coordinates(lat1: Double, lon1: Double, lat2: Double, lon2: Double)

  case class Amount(value: String, currency: String = "RUB")
  case class YaPayment(payment_token: String, amount: Amount, capture: Boolean = true, description: String)
  case class YaToken(token: String, amount: Amount, description: String) {
    def toPayment(capture: Boolean = true) = YaPayment(token, amount, capture, description)
  }

  implicit val coordinatesCodec: Codec[Coordinates] = deriveCodec
  implicit val amountCodec: Codec[Amount] = deriveCodec
  implicit val yaTokenCodec: Codec[YaToken] = deriveCodec
  implicit val yaPaymentCodec: Codec[YaPayment] = deriveCodec
}
