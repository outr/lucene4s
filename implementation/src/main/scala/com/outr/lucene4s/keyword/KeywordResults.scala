package com.outr.lucene4s.keyword

import org.apache.lucene.search.TotalHits

case class KeywordResults(results: List[KeywordResult], total: TotalHits) {
  lazy val words: List[String] = results.map(_.word)
  def highlighted(pre: String = "<strong>", post: String = "</strong>"): List[String] = results.map(_.highlighted(pre, post))
}

object KeywordResults {
  lazy val empty: KeywordResults = KeywordResults(Nil, new TotalHits(0L, TotalHits.Relation.EQUAL_TO))
}