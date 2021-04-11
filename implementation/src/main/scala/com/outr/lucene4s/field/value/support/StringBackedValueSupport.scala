package com.outr.lucene4s.field.value.support

import com.outr.lucene4s.field.Field
import com.outr.lucene4s.field.value.FieldAndValue
import com.outr.lucene4s.query.{PhraseSearchTerm, SearchTerm, TermSearchTerm}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField
import org.apache.lucene.search.SortField.Type
import org.apache.lucene.util.{ByteBlockPool, BytesRef}
import org.apache.lucene.document.{Document, SortedDocValuesField, Field => LuceneField}

trait StringBackedValueSupport[T] extends ValueSupport[T] {
  def toString(value: T): String

  def fromString(s: String): T

  override def store(field: Field[T], value: T, document: Document): Unit = {
    val stored = new LuceneField(field.storeName, toString(value), field.fieldType.lucene())
    document.add(stored)
  }

  override def separateFilter: Boolean = false

  override def filter(field: Field[T], value: T, document: Document): Unit = {}

  override def sorted(field: Field[T], value: T, document: Document): Unit = {
    val bytes = new BytesRef(toString(value))
    if (bytes.length > ByteBlockPool.BYTE_BLOCK_SIZE - 2)
      throw new RuntimeException(s"Value for field ${field.sortName} is greater than " +
        s"${ByteBlockPool.BYTE_BLOCK_SIZE - 2} bytes. " +
        "This would cause a Lucene error. Reduce field size or set: sortable = false")
    val sorted = new SortedDocValuesField(field.sortName, bytes)
    document.add(sorted)
  }

  override def fromLucene(fields: List[IndexableField]): T = fromString(fields.headOption.map(_.stringValue()).orNull)

  override def sortFieldType: Type = SortField.Type.STRING

  override def searchTerm(fv: FieldAndValue[T]): SearchTerm = if (fv.field.fieldType.tokenized) {
    new PhraseSearchTerm(Some(fv.field.asInstanceOf[Field[String]]), toString(fv.value))
  } else {
    new TermSearchTerm(Some(fv.field.asInstanceOf[Field[String]]), toString(fv.value))
  }
}

object StringBackedValueSupport {
  def apply[T](to: T => String, from: String => T): StringBackedValueSupport[T] = new StringBackedValueSupport[T] {
    override def toString(value: T): String = to(value)

    override def fromString(s: String): T = from(s)
  }
}