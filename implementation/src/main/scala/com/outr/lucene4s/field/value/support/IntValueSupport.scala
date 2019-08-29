package com.outr.lucene4s.field.value.support

import com.outr.lucene4s.field.Field
import com.outr.lucene4s.field.value.FieldAndValue
import com.outr.lucene4s.query.{ExactIntSearchTerm, SearchTerm}
import org.apache.lucene.document.{Document, IntPoint, LongPoint, NumericDocValuesField, StoredField}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField
import org.apache.lucene.search.SortField.Type

object IntValueSupport extends ValueSupport[Int] {
  override def store(field: Field[Int], value: Int, document: Document): Unit = {
    val stored = new StoredField(field.storeName, value)
    document.add(stored)
  }

  override def filter(field: Field[Int], value: Int, document: Document): Unit = {
    val filtered = new IntPoint(field.filterName, value)
    document.add(filtered)
  }

  override def sorted(field: Field[Int], value: Int, document: Document): Unit = {
    val sorted = new NumericDocValuesField(field.sortName, value)
    document.add(sorted)
  }

  override def fromLucene(fields: List[IndexableField]): Int = fields.head.numericValue().intValue()

  override def sortFieldType: Type = SortField.Type.INT

  override def searchTerm(fv: FieldAndValue[Int]): SearchTerm = new ExactIntSearchTerm(fv.field, fv.value)
}