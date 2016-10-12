package com.outr.lucene4s.field.value.support

import com.outr.lucene4s.field.Field
import org.apache.lucene.index.IndexableField

trait ValueSupport[T] {
  def toLucene(field: Field[T], value: T): IndexableField

  def fromLucene(field: IndexableField): T
}