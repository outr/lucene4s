package com.outr.lucene4s

import com.outr.lucene4s.document.DocumentBuilder

trait LuceneListener {
  def indexed(builder: DocumentBuilder): Unit
  def delete(): Unit
}
