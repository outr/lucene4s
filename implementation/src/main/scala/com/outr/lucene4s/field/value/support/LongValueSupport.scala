package com.outr.lucene4s.field.value.support

import com.outr.lucene4s.field.Field
import org.apache.lucene.document.{Document, FieldType, IntPoint, LongPoint, NumericDocValuesField, StoredField, Field => LuceneField}
import org.apache.lucene.index.{IndexableField, PointValues}
import org.apache.lucene.search.SortField
import org.apache.lucene.search.SortField.Type

object LongValueSupport extends ValueSupport[Long] {
  override def write(field: Field[Long], value: Long, document: Document): Unit = {
    val stored = new StoredField(field.name, value)
    val filtered = new LongPoint(field.name, value)
    document.add(stored)
    document.add(filtered)
    if (field.sortable) {
      val sorted = new NumericDocValuesField(field.name, value)
      document.add(sorted)
    }
  }

  override def fromLucene(field: IndexableField): Long = field.numericValue().longValue()

  override def sortFieldType: Type = SortField.Type.LONG
}