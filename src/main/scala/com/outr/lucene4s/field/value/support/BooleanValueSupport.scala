package com.outr.lucene4s.field.value.support

import com.outr.lucene4s.field.Field
import org.apache.lucene.document.{IntPoint, StoredField}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField
import org.apache.lucene.search.SortField.Type

object BooleanValueSupport extends ValueSupport[Boolean] {
  override def toLucene(field: Field[Boolean], value: Boolean): IndexableField = {
    new StoredField(field.name, if (value) 1 else 0)
  }

  override def toSortedField(field: Field[Boolean], value: Boolean): Option[IndexableField] = {
    Some(new IntPoint(field.name, if (value) 1 else 0))
  }

  override def fromLucene(field: IndexableField): Boolean = if (field.numericValue().intValue() == 1) true else false

  override def sortFieldType: Type = SortField.Type.INT
}