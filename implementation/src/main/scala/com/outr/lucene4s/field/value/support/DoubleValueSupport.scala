package com.outr.lucene4s.field.value.support

import com.outr.lucene4s.field.Field
import org.apache.lucene.document.{Document, DoubleDocValuesField, DoublePoint, LongPoint, NumericDocValuesField, StoredField}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField
import org.apache.lucene.search.SortField.Type

object DoubleValueSupport extends ValueSupport[Double] {
  override def write(field: Field[Double], value: Double, document: Document): Unit = {
    val stored = new StoredField(field.name, value)
    val filtered = new DoublePoint(field.name, value)
    document.add(stored)
    document.add(filtered)
    if (field.sortable) {
      val sorted = new DoubleDocValuesField(field.name, value)
      document.add(sorted)
    }
  }

  override def fromLucene(field: IndexableField): Double = field.numericValue().doubleValue()

  override def sortFieldType: Type = SortField.Type.DOUBLE
}