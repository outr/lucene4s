package com.outr.lucene4s.field.value.support

import com.outr.lucene4s.field.Field
import org.apache.lucene.document.SortedDocValuesField
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField

trait ValueSupport[T] {
  def toLucene(field: Field[T], value: T): IndexableField

  def toSortedField(field: Field[T], value: T): Option[IndexableField]

  def fromLucene(field: IndexableField): T

  def sortFieldType: SortField.Type
}