package com.outr.lucene4s.field.value

import com.outr.lucene4s.field.Field
import org.apache.lucene.document.Document

class FieldAndValue[T](field: Field[T], value: T) {
  def write(document: Document): Unit = {
    val f = field.support.toLucene(field, value)
    document.add(f)
  }
}