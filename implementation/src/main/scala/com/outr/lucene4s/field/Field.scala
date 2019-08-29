package com.outr.lucene4s.field

import com.outr.lucene4s.field.value.FieldAndValue
import com.outr.lucene4s.field.value.support.ValueSupport

case class Field[T](private val name: String,
                    fieldType: FieldType,
                    support: ValueSupport[T],
                    fullTextSearchable: Boolean,
                    sortable: Boolean = true) {
  def apply(value: T): FieldAndValue[T] = FieldAndValue[T](this, value)

  def storeName: String = name
  lazy val filterName: String = if (support.separateFilter) s"${name}Filter" else storeName
  lazy val sortName: String = s"${name}Sort"

  override def toString: String = name
}