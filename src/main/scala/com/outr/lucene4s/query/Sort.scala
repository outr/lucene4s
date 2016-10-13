package com.outr.lucene4s.query

import com.outr.lucene4s.field.Field
import org.apache.lucene.search.SortField

sealed trait Sort {
  protected[lucene4s] def sortField(): SortField
}

case class FieldSort[T](field: Field[T], reverse: Boolean) extends Sort {
  override protected[lucene4s] def sortField(): SortField = {
    new SortField(field.name, field.support.sortFieldType, reverse)
  }
}

object Sort {
  case object Score extends Sort {
    override protected[lucene4s] def sortField(): SortField = SortField.FIELD_SCORE
  }

  case object IndexOrder extends Sort {
    override protected[lucene4s] def sortField(): SortField = SortField.FIELD_DOC
  }

  def apply[T](field: Field[T], reverse: Boolean = false): Sort = FieldSort[T](field, reverse)
}