package com.outr.lucene4s.field.value.support

import com.outr.lucene4s.field.Field
import org.apache.lucene.document.{Document, IntPoint, NumericDocValuesField, StoredField}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField
import org.apache.lucene.search.SortField.Type

object BooleanValueSupport extends ValueSupport[Boolean] {
  override def write(field: Field[Boolean], value: Boolean, document: Document): Unit = {
    val v = if (value) 1 else 0
    val stored = new StoredField(field.name, v)
    val filtered = new IntPoint(field.name, v)
    document.add(stored)
    document.add(filtered)
    if (field.sortable) {
      val sorted = new NumericDocValuesField(field.name, v)
      document.add(sorted)
    }
  }

  override def fromLucene(field: IndexableField): Boolean = if (field.numericValue().intValue() == 1) true else false

  override def sortFieldType: Type = SortField.Type.INT
}