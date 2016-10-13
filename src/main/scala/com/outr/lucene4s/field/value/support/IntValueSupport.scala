package com.outr.lucene4s.field.value.support

import com.outr.lucene4s.field.Field
import org.apache.lucene.document.{IntPoint, SortedDocValuesField, Field => LuceneField}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField
import org.apache.lucene.search.SortField.Type

// TODO: test this and implement sorted field
object IntValueSupport extends ValueSupport[Int] {
  override def toLucene(field: Field[Int], value: Int): IndexableField = {
    new IntPoint(field.name, value)
  }

  override def toSortedField(field: Field[Int], value: Int): Option[SortedDocValuesField] = None

  override def fromLucene(field: IndexableField): Int = field.numericValue().intValue()

  override def sortFieldType: Type = SortField.Type.INT
}