package com.outr.lucene4s.field.value.support

import com.outr.lucene4s.field.Field
import com.outr.lucene4s.field.value.SpatialPoint
import org.apache.lucene.document.{Document, LatLonDocValuesField, LatLonPoint, StoredField}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField
import org.apache.lucene.search.SortField.Type

object SpatialPointValueSupport extends ValueSupport[SpatialPoint] {
  override def write(field: Field[SpatialPoint], value: SpatialPoint, document: Document): Unit = {
    val stored = new StoredField(field.name, value.toString)
    val filtered = new LatLonPoint(field.name, value.latitude, value.longitude)
    val sorted = new LatLonDocValuesField(field.name, value.latitude, value.longitude)
    document.add(stored)
    document.add(filtered)
    document.add(sorted)
  }

  override def fromLucene(field: IndexableField): SpatialPoint = SpatialPoint.fromString(field.stringValue())

  override def sortFieldType: Type = SortField.Type.SCORE
}