package com.outr.lucene4s.field.value.support

import com.outr.lucene4s.field.Field
import com.outr.lucene4s.field.value.FieldAndValue
import com.outr.lucene4s.query.{MatchAllSearchTerm, SearchTerm}
import org.apache.lucene.document.Document
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField.Type

class OptionValueSupport[T](underlying: ValueSupport[T]) extends ValueSupport[Option[T]] {
  override def store(field: Field[Option[T]], value: Option[T], document: Document): Unit = {
    value.foreach(underlying.store(field.asInstanceOf[Field[T]], _, document))
  }

  override def filter(field: Field[Option[T]], value: Option[T], document: Document): Unit = {
    value.foreach(underlying.filter(field.asInstanceOf[Field[T]], _, document))
  }

  override def sorted(field: Field[Option[T]], value: Option[T], document: Document): Unit = {
    value.foreach(underlying.sorted(field.asInstanceOf[Field[T]], _, document))
  }

  override def fromLucene(fields: List[IndexableField]): Option[T] = Option(underlying.fromLucene(fields))

  override def sortFieldType: Type = underlying.sortFieldType

  override def searchTerm(fv: FieldAndValue[Option[T]]): SearchTerm = {
    fv.value.map { t =>
      underlying.searchTerm(FieldAndValue(fv.field.asInstanceOf[Field[T]], t))
    }.getOrElse(MatchAllSearchTerm)
  }
}