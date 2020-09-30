package com.mitrakov.self.uber

import java.time.OffsetDateTime
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

package object yandex {
  case class CommonResponse(data: String, errorCode: Int = 0, errorMessage: String = "")
  case class Amount(value: String, currency: String = "RUB")
  case class PaymentRequest(payment_token: String, amount: Amount, capture: Boolean = true, description: String)
  case class Token(token: String, amount: Amount, description: String) {
    def toPayment(capture: Boolean = true) = PaymentRequest(token, amount, capture, description)
  }
  case class Card(first6: String, last4: String, expiry_month: String, expiry_year: String, card_type: String, issuer_country: String, issuer_name: String)
  case class Confirmation(`type`: String, return_url: Option[String], confirmation_url: Option[String])
  case class Metadata(scid: Option[String])
  case class PaymentMethod(`type`: String, id: String, saved: Boolean, card: Option[Card], title: Option[String])
  case class Recipient(account_id: String, gateway_id: String)
  case class PaymentResponse(
                              id: String,
                              status: String,
                              paid: Boolean,
                              amount: Amount,
                              confirmation: Option[Confirmation],
                              created_at: OffsetDateTime,
                              description: Option[String],
                              metadata: Option[Metadata],
                              payment_method: PaymentMethod,
                              recipient: Recipient,
                              refundable: Boolean,
                              test: Boolean,
  )

  implicit val commonResponseCodec: Codec[CommonResponse] = deriveCodec
  implicit val amountCodec: Codec[Amount] = deriveCodec
  implicit val paymentRequestCodec: Codec[PaymentRequest] = deriveCodec
  implicit val tokenCodec: Codec[Token] = deriveCodec
  implicit val cardCodec: Codec[Card] = deriveCodec
  implicit val confirmationCodec: Codec[Confirmation] = deriveCodec
  implicit val metadataCodec: Codec[Metadata] = deriveCodec
  implicit val paymentMethodCodec: Codec[PaymentMethod] = deriveCodec
  implicit val recipientCodec: Codec[Recipient] = deriveCodec
  implicit val paymentResponseCodec: Codec[PaymentResponse] = deriveCodec
}
