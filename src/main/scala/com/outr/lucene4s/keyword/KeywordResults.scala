package com.outr.lucene4s.keyword

case class KeywordResults(results: List[KeywordResult], total: Long, maxScore: Double) {
  lazy val words: List[String] = results.map(_.word)
  def highlighted(pre: String = "<strong>", post: String = "</strong>"): List[String] = results.map(_.highlighted(pre, post))
}

object KeywordResults {
  lazy val empty: KeywordResults = KeywordResults(Nil, 0L, 0.0)
}