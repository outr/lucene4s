package com.outr.lucene4s.field.value.support

import com.outr.lucene4s.field.Field
import com.outr.lucene4s.field.value.FieldAndValue
import com.outr.lucene4s.query.{ExactLongSearchTerm, SearchTerm}
import org.apache.lucene.document.{Document, LongPoint, NumericDocValuesField, StoredField}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField
import org.apache.lucene.search.SortField.Type

object LongValueSupport extends ValueSupport[Long] {
  override def store(field: Field[Long], value: Long, document: Document): Unit = {
    val stored = new StoredField(field.name, value)
    document.add(stored)
  }

  override def filter(field: Field[Long], value: Long, document: Document): Unit = {
    val filtered = new LongPoint(field.name, value)
    document.add(filtered)
  }

  override def sorted(field: Field[Long], value: Long, document: Document): Unit = {
    val sorted = new NumericDocValuesField(field.name, value)
    document.add(sorted)
  }

  override def fromLucene(fields: List[IndexableField]): Long = fields.head.numericValue().longValue()

  override def sortFieldType: Type = SortField.Type.LONG

  override def searchTerm(fv: FieldAndValue[Long]): SearchTerm = new ExactLongSearchTerm(fv.field, fv.value)
}