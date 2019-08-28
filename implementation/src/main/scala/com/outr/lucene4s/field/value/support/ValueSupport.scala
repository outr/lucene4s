package com.outr.lucene4s.field.value.support

import com.outr.lucene4s.field.Field
import com.outr.lucene4s.field.value.FieldAndValue
import com.outr.lucene4s.query.SearchTerm
import org.apache.lucene.document.Document
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField

trait ValueSupport[T] {
  def store(field: Field[T], value: T, document: Document): Unit

  def filter(field: Field[T], value: T, document: Document): Unit

  def sorted(field: Field[T], value: T, document: Document): Unit

  def fromLucene(fields: List[IndexableField]): T

  def sortFieldType: SortField.Type

  def searchTerm(fv: FieldAndValue[T]): SearchTerm
}