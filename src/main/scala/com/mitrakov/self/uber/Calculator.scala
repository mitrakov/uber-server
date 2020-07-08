package com.mitrakov.self.uber

object Calculator {
  def getPrices(coords: Coordinates): Map[String, Double] = {
    val distance = getDistance(coords.lat1, coords.lon1, coords.lat2, coords.lon2)
    val x = 61.1

    Map("Uber-X" -> x * distance)
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
