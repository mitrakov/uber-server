package com.mitrakov.self.uber

import scala.collection.mutable

object Calculator {
  def getPrices(coords: Coordinates, discount: Double = 1.0): mutable.LinkedHashMap[String, Int] = {
    val distance = getDistance(coords.lat1, coords.lon1, coords.lat2, coords.lon2)
    val uberX = 61.1
    val select = 128.2
    val `select+` = 133.2
    val black = 252.8
    val lux = 617.1
    val van = 292.5
    val kids = 117.1 * discount

    mutable.LinkedHashMap(
      "UberX" -> (distance * uberX).toInt,
      "Select" -> (distance * select).toInt,
      "Select+" -> (distance * `select+`).toInt,
      "Black" -> (distance * black).toInt,
      "Lux" -> (distance * lux).toInt,
      "Van" -> (distance * van).toInt,
      "Kids" -> (distance * kids).toInt,
      "Kids+" -> (distance * kids).toInt,
    )
  }

  /**
   * @see https://www.movable-type.co.uk/scripts/latlong.html
   */
  private def getDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double = {
    import math._
    def toRadian(degrees: Double): Double = degrees * Pi / 180
    val earthRadius = 6371.0088

    val dLat = toRadian(lat2 - lat1)
    val dLon = toRadian(lon2 - lon1)

    val a = sin(dLat/2) * sin(dLat/2) + sin(dLon/2) * sin(dLon/2) * cos(toRadian(lat1)) * cos(toRadian(lat2))
    val c = 2 * atan2(sqrt(a), sqrt(1-a))

    c * earthRadius
  }
}
