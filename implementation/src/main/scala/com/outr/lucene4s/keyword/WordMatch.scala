package com.outr.lucene4s.keyword

import com.outr.lucene4s.Lucene

trait WordMatch {
  def isMatch: Boolean
  def highlighted(matchStart: String = "<strong>", matchEnd: String = "</strong>"): String
}

case class WordMatched(pre: String, matched: String, post: String) extends WordMatch {
  override def isMatch: Boolean = true
  override def highlighted(matchStart: String = "<strong>", matchEnd: String = "</strong>"): String = {
    s"$pre$matchStart$matched$matchEnd$post"
  }
}

case class WordNonMatch(word: String) extends WordMatch {
  override def isMatch: Boolean = false
  override def highlighted(matchStart: String, matchEnd: String): String = word
}

object WordMatch {
  def apply(queryString: String, word: String): WordMatch = {
    val matches = Lucene.queryToWords(queryString).map { queryWord =>
      val qw = queryWord.toLowerCase
      val wordLowerCase = word.toLowerCase
      val index = if (qw.nonEmpty && wordLowerCase.contains(qw)) {
        wordLowerCase.indexOf(qw)
      } else {
        -1
      }
      if (index > -1) {
        val pre = word.substring(0, index)
        val matched = word.substring(index, index + qw.length)
        val post = word.substring(index + qw.length)
        WordMatched(pre, matched, post)
      } else {
        WordNonMatch(word)
      }
    }
    matches.find(_.isMatch).orElse(matches.headOption).getOrElse(WordNonMatch(word))
  }
}