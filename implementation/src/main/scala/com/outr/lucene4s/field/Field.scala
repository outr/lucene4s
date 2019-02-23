package com.outr.lucene4s.field

import com.outr.lucene4s.field.value.FieldAndValue
import com.outr.lucene4s.field.value.support.ValueSupport
import com.outr.lucene4s.query.SearchTerm

case class Field[T](name: String,
                    fieldType: FieldType,
                    support: ValueSupport[T],
                    fullTextSearchable: Boolean,
                    sortable: Boolean = true)
                   (implicit val fv2SearchTerm: FieldAndValue[T] => SearchTerm) {
  def apply(value: T): FieldAndValue[T] = FieldAndValue[T](this, value)
}