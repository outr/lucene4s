package com.outr.lucene4s.field.value.support

import com.outr.lucene4s.field.Field
import com.outr.lucene4s.field.value.FieldAndValue
import com.outr.lucene4s.query.{PhraseSearchTerm, SearchTerm, TermSearchTerm}
import org.apache.lucene.document.{Document, SortedDocValuesField, Field => LuceneField}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField
import org.apache.lucene.search.SortField.Type
import org.apache.lucene.util.{ByteBlockPool, BytesRef}

object StringValueSupport extends ValueSupport[String] {
  override def store(field: Field[String], value: String, document: Document): Unit = {
    val stored = new LuceneField(field.name, value, field.fieldType.lucene())
    document.add(stored)
  }

  override def filter(field: Field[String], value: String, document: Document): Unit = {}

  override def sorted(field: Field[String], value: String, document: Document): Unit = {
    val bytes = new BytesRef(value)
    if (bytes.length > ByteBlockPool.BYTE_BLOCK_SIZE - 2)
      throw new RuntimeException(s"Value for field ${field.name} is greater than " +
        s"${ByteBlockPool.BYTE_BLOCK_SIZE - 2} bytes. " +
        "This would cause a Lucene error. Reduce field size or set: sortable = false")
    val sorted = new SortedDocValuesField(field.name, bytes)
    document.add(sorted)
  }

  override def fromLucene(fields: List[IndexableField]): String = fields.headOption.map(_.stringValue()).orNull

  override def sortFieldType: Type = SortField.Type.STRING

  override def searchTerm(fv: FieldAndValue[String]): SearchTerm = {
    if (fv.field.fieldType.tokenized) {
      new PhraseSearchTerm(Some(fv.field), fv.value)
    } else {
      new TermSearchTerm(Some(fv.field), fv.value)
    }
  }
}