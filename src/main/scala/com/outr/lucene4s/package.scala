package com.outr

import com.outr.lucene4s.field.value.support._

import scala.language.implicitConversions

package object lucene4s {
  implicit def stringSupport: ValueSupport[String] = StringValueSupport
  implicit def intSupport: ValueSupport[Int] = IntValueSupport
}