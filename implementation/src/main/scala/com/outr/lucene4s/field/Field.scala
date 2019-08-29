package com.outr.lucene4s.field

import com.outr.lucene4s.field.value.FieldAndValue
import com.outr.lucene4s.field.value.support.ValueSupport

case class Field[T](name: String,
                    fieldType: FieldType,
                    support: ValueSupport[T],
                    fullTextSearchable: Boolean,
                    sortable: Boolean = true) {
  def apply(value: T): FieldAndValue[T] = FieldAndValue[T](this, value)

  override def toString: String = name
}