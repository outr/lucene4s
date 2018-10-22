package com.outr.lucene4s.field.value.support

import com.outr.lucene4s.field.Field
import org.apache.lucene.document.{Document, IntPoint, LongPoint, NumericDocValuesField, StoredField}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField
import org.apache.lucene.search.SortField.Type

object IntValueSupport extends ValueSupport[Int] {
  override def write(field: Field[Int], value: Int, document: Document): Unit = {
    val stored = new StoredField(field.name, value)
    val filtered = new IntPoint(field.name, value)
    document.add(stored)
    document.add(filtered)
    if (field.sortable) {
      val sorted = new NumericDocValuesField(field.name, value)
      document.add(sorted)
    }
  }

  override def fromLucene(field: IndexableField): Int = field.numericValue().intValue()

  override def sortFieldType: Type = SortField.Type.INT
}