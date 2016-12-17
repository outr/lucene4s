package com.outr.lucene4s.field.value

case class SpatialPoint(latitude: Double, longitude: Double) {
  override def toString: String = s"${latitude}x${longitude}"
}

object SpatialPoint {
  def fromString(s: String): SpatialPoint = {
    val index = s.indexOf('x')
    val latitude = s.substring(0, index).toDouble
    val longitude = s.substring(index + 1).toDouble
    SpatialPoint(latitude, longitude)
  }
}