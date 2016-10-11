package com.outr

import org.apache.lucene.util.BytesRef

import scala.language.implicitConversions

package object lucene4s {
  implicit def string2FieldValue(value: String): FieldValue = StringFieldValue(value)
  implicit def byteArray2FieldValue(value: Array[Byte]): FieldValue = ByteArrayFieldValue(value)
  implicit def bytesRef2FieldValue(value: BytesRef): FieldValue = BytesRefFieldValue(value)
}