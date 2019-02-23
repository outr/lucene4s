package com.outr.lucene4s.query

import com.outr.lucene4s.field.Field
import com.outr.lucene4s.field.value.SpatialPoint
import org.apache.lucene.document.LatLonDocValuesField
import org.apache.lucene.search.SortField

trait Sort {
  protected[lucene4s] def sortField(): SortField
}

case class FieldSort[T](field: Field[T], reverse: Boolean) extends Sort {
  override protected[lucene4s] def sortField(): SortField = {
    new SortField(field.name, field.support.sortFieldType, reverse)
  }
}

case class NearestSort(field: Field[SpatialPoint], point: SpatialPoint) extends Sort {
  override protected[lucene4s] def sortField(): SortField = LatLonDocValuesField.newDistanceSort(field.name, point.latitude, point.longitude)
}

object Sort {
  case object Score extends Sort {
    override protected[lucene4s] def sortField(): SortField = SortField.FIELD_SCORE
  }

  case object IndexOrder extends Sort {
    override protected[lucene4s] def sortField(): SortField = SortField.FIELD_DOC
  }

  def apply[T](field: Field[T], reverse: Boolean = false): Sort = FieldSort[T](field, reverse)

  def nearest(field: Field[SpatialPoint], point: SpatialPoint): Sort = NearestSort(field, point)
}