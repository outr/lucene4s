package com.outr.lucene4s.field

import com.outr.lucene4s.field.value.support.ValueSupport
import com.outr.lucene4s.field.value.FieldAndValue

case class Field[T](name: String, fieldType: FieldType, support: ValueSupport[T]) {
  def apply(value: T): FieldAndValue[T] = new FieldAndValue[T](this, value)
}