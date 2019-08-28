package com.outr.lucene4s.field.value

import com.outr.lucene4s.field.Field
import org.apache.lucene.document.Document

case class FieldAndValue[T](field: Field[T], value: T) {
  def write(document: Document): Unit = {
    field.support.store(field, value, document)
    field.support.filter(field, value, document)
    if (field.sortable) {
      field.support.sorted(field, value, document)
    }
  }
}