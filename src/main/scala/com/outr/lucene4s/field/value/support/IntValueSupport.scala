package com.outr.lucene4s.field.value.support

import com.outr.lucene4s.field.Field
import org.apache.lucene.document.{IntPoint, StoredField}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField
import org.apache.lucene.search.SortField.Type

object IntValueSupport extends ValueSupport[Int] {
  override def toLucene(field: Field[Int], value: Int): IndexableField = {
    new StoredField(field.name, value)
  }

  override def toSortedField(field: Field[Int], value: Int): Option[IndexableField] = {
    Some(new IntPoint(field.name, value))
  }

  override def fromLucene(field: IndexableField): Int = field.numericValue().intValue()

  override def sortFieldType: Type = SortField.Type.INT
}