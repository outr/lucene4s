package com.outr.lucene4s.field.value.support

import com.outr.lucene4s.Stringify
import com.outr.lucene4s.field.Field
import com.outr.lucene4s.field.value.FieldAndValue
import com.outr.lucene4s.query.SearchTerm
import org.apache.lucene.document.Document
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField

case class StringifyValueSupport[T](stringify: Stringify[T]) extends ValueSupport[T] {
  override def store(field: Field[T], value: T, document: Document): Unit = {
    StringValueSupport.store(field.asInstanceOf[Field[String]], stringify.toString(value), document)
  }

  override def filter(field: Field[T], value: T, document: Document): Unit = {
    StringValueSupport.filter(field.asInstanceOf[Field[String]], stringify.toString(value), document)
  }

  override def sorted(field: Field[T], value: T, document: Document): Unit = {
    StringValueSupport.sorted(field.asInstanceOf[Field[String]], stringify.toString(value), document)
  }

  override def fromLucene(fields: List[IndexableField]): T = {
    stringify.fromString(StringValueSupport.fromLucene(fields))
  }

  override def separateFilter: Boolean = StringValueSupport.separateFilter

  override def sortFieldType: SortField.Type = StringValueSupport.sortFieldType

  override def searchTerm(fv: FieldAndValue[T]): SearchTerm = {
    StringValueSupport.searchTerm(FieldAndValue(fv.field.asInstanceOf[Field[String]], stringify.toString(fv.value)))
  }
}