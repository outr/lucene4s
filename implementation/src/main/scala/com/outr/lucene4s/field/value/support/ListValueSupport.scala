package com.outr.lucene4s.field.value.support

import com.outr.lucene4s._
import com.outr.lucene4s.field.Field
import com.outr.lucene4s.field.value.FieldAndValue
import com.outr.lucene4s.query.SearchTerm
import org.apache.lucene.document.Document
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField.Type

class ListValueSupport[T](underlying: ValueSupport[T]) extends ValueSupport[List[T]] {
  override def store(field: Field[List[T]], value: List[T], document: Document): Unit = value.foreach { t =>
    underlying.store(field.asInstanceOf[Field[T]], t, document)
  }

  override def filter(field: Field[List[T]], value: List[T], document: Document): Unit = value.foreach { t =>
    underlying.filter(field.asInstanceOf[Field[T]], t, document)
  }

  // TODO: Better handling of single value sorting
  override def sorted(field: Field[List[T]], value: List[T], document: Document): Unit = value.headOption.foreach { t =>
    underlying.sorted(field.asInstanceOf[Field[T]], t, document)
  }

  override def fromLucene(fields: List[IndexableField]): List[T] = {
    fields.grouped(1).toList.map(underlying.fromLucene)
  }

  override def sortFieldType: Type = underlying.sortFieldType

  override def searchTerm(fv: FieldAndValue[List[T]]): SearchTerm = {
    all(fv.value.map(v => underlying.searchTerm(FieldAndValue(fv.field.asInstanceOf[Field[T]], v))): _*)
  }
}
