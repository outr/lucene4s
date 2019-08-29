package com.outr.lucene4s.field.value.support

import com.outr.lucene4s.field.Field
import com.outr.lucene4s.field.value.FieldAndValue
import com.outr.lucene4s.query.{ExactBooleanSearchTerm, SearchTerm}
import org.apache.lucene.document.{Document, IntPoint, NumericDocValuesField, StoredField}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField
import org.apache.lucene.search.SortField.Type

object BooleanValueSupport extends ValueSupport[Boolean] {
  override def store(field: Field[Boolean], value: Boolean, document: Document): Unit = {
    val v = if (value) 1 else 0
    val stored = new StoredField(field.storeName, v)
    document.add(stored)
  }

  override def filter(field: Field[Boolean], value: Boolean, document: Document): Unit = {
    val v = if (value) 1 else 0
    val filtered = new IntPoint(field.filterName, v)
    document.add(filtered)
  }

  override def sorted(field: Field[Boolean], value: Boolean, document: Document): Unit = {
    val v = if (value) 1 else 0
    val sorted = new NumericDocValuesField(field.sortName, v)
    document.add(sorted)
  }

  override def fromLucene(fields: List[IndexableField]): Boolean = if (fields.head.numericValue().intValue() == 1) true else false

  override def sortFieldType: Type = SortField.Type.INT

  override def searchTerm(fv: FieldAndValue[Boolean]): SearchTerm = new ExactBooleanSearchTerm(fv.field, fv.value)
}