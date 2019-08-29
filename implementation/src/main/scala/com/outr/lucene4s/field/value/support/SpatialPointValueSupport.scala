package com.outr.lucene4s.field.value.support

import com.outr.lucene4s._
import com.outr.lucene4s.field.Field
import com.outr.lucene4s.field.value.{FieldAndValue, SpatialPoint}
import com.outr.lucene4s.query.SearchTerm
import org.apache.lucene.document.{Document, LatLonDocValuesField, LatLonPoint, StoredField}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField
import org.apache.lucene.search.SortField.Type

object SpatialPointValueSupport extends ValueSupport[SpatialPoint] {
  override def store(field: Field[SpatialPoint], value: SpatialPoint, document: Document): Unit = {
    val stored = new StoredField(field.storeName, value.toString)
    document.add(stored)
  }

  override def filter(field: Field[SpatialPoint], value: SpatialPoint, document: Document): Unit = {
    val filtered = new LatLonPoint(field.filterName, value.latitude, value.longitude)
    document.add(filtered)
  }

  override def sorted(field: Field[SpatialPoint], value: SpatialPoint, document: Document): Unit = {
    val sorted = new LatLonDocValuesField(field.sortName, value.latitude, value.longitude)
    document.add(sorted)
  }

  override def fromLucene(fields: List[IndexableField]): SpatialPoint = SpatialPoint.fromString(fields.head.stringValue())

  override def sortFieldType: Type = SortField.Type.SCORE

  override def searchTerm(fv: FieldAndValue[SpatialPoint]): SearchTerm = spatialDistance(fv.field, fv.value, 1.meters)
}