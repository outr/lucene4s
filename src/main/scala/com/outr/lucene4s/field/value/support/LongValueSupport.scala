package com.outr.lucene4s.field.value.support

import com.outr.lucene4s.field.Field
import org.apache.lucene.document.{IntPoint, LongPoint, StoredField}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField
import org.apache.lucene.search.SortField.Type

object LongValueSupport extends ValueSupport[Long] {
  override def toLucene(field: Field[Long], value: Long): IndexableField = {
    new StoredField(field.name, value)
  }

  override def toSortedField(field: Field[Long], value: Long): Option[IndexableField] = {
    Some(new LongPoint(field.name, value))
  }

  override def fromLucene(field: IndexableField): Long = field.numericValue().longValue()

  override def sortFieldType: Type = SortField.Type.LONG
}