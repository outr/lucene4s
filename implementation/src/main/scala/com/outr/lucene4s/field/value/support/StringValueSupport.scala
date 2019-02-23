package com.outr.lucene4s.field.value.support

import com.outr.lucene4s.field.Field
import org.apache.lucene.document.{Document, SortedDocValuesField, StringField, Field => LuceneField}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField
import org.apache.lucene.search.SortField.Type
import org.apache.lucene.util.{ByteBlockPool, BytesRef}

object StringValueSupport extends ValueSupport[String] {
  override def write(field: Field[String], value: String, document: Document): Unit = {
    val stored = new LuceneField(field.name, value, field.fieldType.lucene())
    document.add(stored)
    if (field.sortable) {
      val bytes = new BytesRef(value)
      if (bytes.length > ByteBlockPool.BYTE_BLOCK_SIZE - 2)
        throw new RuntimeException(s"Value for field ${field.name} is greater than " +
          s"${ByteBlockPool.BYTE_BLOCK_SIZE - 2} bytes. " +
          "This would cause a Lucene error. Reduce field size or set: sortable = false")
      val sorted = new SortedDocValuesField(field.name, bytes)
      document.add(sorted)
    }
  }

  override def fromLucene(field: IndexableField): String = field.stringValue()

  override def sortFieldType: Type = SortField.Type.STRING
}