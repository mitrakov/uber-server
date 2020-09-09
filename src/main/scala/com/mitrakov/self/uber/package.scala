package com.mitrakov.self

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec

package object uber {
  case class Coordinates(lat1: Double, lon1: Double, lat2: Double, lon2: Double)

  implicit val coordinatesCodec: Codec[Coordinates] = deriveCodec
}
