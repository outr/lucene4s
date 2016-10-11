package com.outr.lucene4s.document

import com.outr.lucene4s.Lucene
import com.outr.lucene4s.field.FieldType
import com.outr.lucene4s.field.value.{ByteArrayFieldValue, BytesRefFieldValue, FieldValue, StringFieldValue}
import org.apache.lucene.document.{Document, Field}

class DocumentBuilder(lucene: Lucene, document: Document = new Document) {
  def field(name: String, value: FieldValue, fieldType: FieldType = FieldType.Stored): DocumentBuilder = {
    val ft = fieldType.lucene()
    val f = value match {
      case StringFieldValue(v) => new Field(name, v, ft)
      case ByteArrayFieldValue(v) => new Field(name, v, ft)
      case BytesRefFieldValue(v) => new Field(name, v, ft)
    }
    println(s"Field: $f")
    document.add(f)
    this
  }

  def index(): Unit = lucene.store(document)
}
