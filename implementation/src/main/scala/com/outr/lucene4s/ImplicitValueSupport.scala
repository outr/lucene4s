package com.outr.lucene4s

import com.outr.lucene4s.field.value.SpatialPoint
import com.outr.lucene4s.field.value.support._

trait ImplicitValueSupport {
  implicit def stringSupport: ValueSupport[String] = StringValueSupport
  implicit def booleanSupport: ValueSupport[Boolean] = BooleanValueSupport
  implicit def intSupport: ValueSupport[Int] = IntValueSupport
  implicit def longSupport: ValueSupport[Long] = LongValueSupport
  implicit def doubleSupport: ValueSupport[Double] = DoubleValueSupport
  implicit def spatialPointSupport: ValueSupport[SpatialPoint] = SpatialPointValueSupport

  implicit val listStringSupport: ValueSupport[List[String]] = new ListValueSupport[String](stringSupport)
  implicit val listBooleanSupport: ValueSupport[List[Boolean]] = new ListValueSupport[Boolean](booleanSupport)
  implicit val listIntSupport: ValueSupport[List[Int]] = new ListValueSupport[Int](intSupport)
  implicit val listLongSupport: ValueSupport[List[Long]] = new ListValueSupport[Long](longSupport)
  implicit val listDoubleSupport: ValueSupport[List[Double]] = new ListValueSupport[Double](doubleSupport)
  implicit val listSpatialSupport: ValueSupport[List[SpatialPoint]] = new ListValueSupport[SpatialPoint](spatialPointSupport)

  implicit val optionStringSupport: ValueSupport[Option[String]] = new OptionValueSupport[String](stringSupport)
  implicit val optionBooleanSupport: ValueSupport[Option[Boolean]] = new OptionValueSupport[Boolean](booleanSupport)
  implicit val optionIntSupport: ValueSupport[Option[Int]] = new OptionValueSupport[Int](intSupport)
  implicit val optionLongSupport: ValueSupport[Option[Long]] = new OptionValueSupport[Long](longSupport)
  implicit val optionDoubleSupport: ValueSupport[Option[Double]] = new OptionValueSupport[Double](doubleSupport)
  implicit val optionSpatialSupport: ValueSupport[Option[SpatialPoint]] = new OptionValueSupport[SpatialPoint](spatialPointSupport)
}