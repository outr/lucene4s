package com.outr.lucene4s.field.value.support

object StringValueSupport extends StringBackedValueSupport[String] {
  override def toString(value: String): String = value

  override def fromString(s: String): String = s
}