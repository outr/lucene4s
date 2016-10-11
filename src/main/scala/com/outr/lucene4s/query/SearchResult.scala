package com.outr.lucene4s.query

import com.outr.lucene4s.Lucene
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.util.BytesRef

class SearchResult private[lucene4s](lucene: Lucene, search: SearchResults, scoreDoc: ScoreDoc) {
  private lazy val doc = lucene.searcher.doc(scoreDoc.doc)

  def string(name: String): String = doc.getField(name).stringValue()
  def bytesRef(name: String): BytesRef = doc.getField(name).binaryValue()
  def numeric(name: String): Number = doc.getField(name).numericValue()
  def int(name: String): Int = numeric(name).intValue()
  def double(name: String): Double = numeric(name).doubleValue()
}
