package com.outr.lucene4s.field.value.support

import com.outr.lucene4s.field.Field
import com.outr.lucene4s.field.value.FieldAndValue
import com.outr.lucene4s.query.{ExactDoubleSearchTerm, SearchTerm}
import org.apache.lucene.document.{Document, DoubleDocValuesField, DoublePoint, LongPoint, NumericDocValuesField, StoredField}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField
import org.apache.lucene.search.SortField.Type

object DoubleValueSupport extends ValueSupport[Double] {
  override def store(field: Field[Double], value: Double, document: Document): Unit = {
    val stored = new StoredField(field.name, value)
    document.add(stored)
  }

  override def filter(field: Field[Double], value: Double, document: Document): Unit = {
    val filtered = new DoublePoint(field.name, value)
    document.add(filtered)
  }

  override def sorted(field: Field[Double], value: Double, document: Document): Unit = {
    val sorted = new DoubleDocValuesField(field.name, value)
    document.add(sorted)
  }

  override def fromLucene(fields: List[IndexableField]): Double = fields.head.numericValue().doubleValue()

  override def sortFieldType: Type = SortField.Type.DOUBLE

  override def searchTerm(fv: FieldAndValue[Double]): SearchTerm = new ExactDoubleSearchTerm(fv.field, fv.value)
}