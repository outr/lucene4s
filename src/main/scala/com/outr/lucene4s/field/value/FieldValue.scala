package com.outr.lucene4s.field.value

import org.apache.lucene.util.BytesRef

sealed trait FieldValue

case class StringFieldValue(value: String) extends FieldValue

case class ByteArrayFieldValue(value: Array[Byte]) extends FieldValue

case class BytesRefFieldValue(value: BytesRef) extends FieldValue