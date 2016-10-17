package com.outr.lucene4s.field.value.support

import com.outr.lucene4s.field.Field
import org.apache.lucene.document.{Document, SortedDocValuesField, Field => LuceneField}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField
import org.apache.lucene.search.SortField.Type
import org.apache.lucene.util.BytesRef

object StringValueSupport extends ValueSupport[String] {
  override def write(field: Field[String], value: String, document: Document): Unit = {
    val stored = new LuceneField(field.name, value, field.fieldType.lucene())
    val sorted = new SortedDocValuesField(field.name, new BytesRef(value))
    document.add(stored)
    document.add(sorted)
  }

  override def fromLucene(field: IndexableField): String = field.stringValue()

  override def sortFieldType: Type = SortField.Type.STRING
}