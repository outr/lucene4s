package com.outr.lucene4s.keyword

case class KeywordResult(word: String,
                         wordMatch: WordMatch,
                         score: Double,
                         additionalFields: Map[String, String]) {
  def highlighted(pre: String = "<strong>", post: String = "</strong>"): String = {
    wordMatch.highlighted(pre, post)
  }
}