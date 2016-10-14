package com.outr.lucene4s.field.value.support

import com.outr.lucene4s.field.Field
import org.apache.lucene.document.{DoublePoint, StoredField}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField
import org.apache.lucene.search.SortField.Type

object DoubleValueSupport extends ValueSupport[Double] {
  override def toLucene(field: Field[Double], value: Double): IndexableField = {
    new StoredField(field.name, value)
  }

  override def toSortedField(field: Field[Double], value: Double): Option[IndexableField] = {
    Some(new DoublePoint(field.name, value))
  }

  override def fromLucene(field: IndexableField): Double = field.numericValue().doubleValue()

  override def sortFieldType: Type = SortField.Type.DOUBLE
}