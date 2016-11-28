package com.outr.lucene4s.query

import com.outr.lucene4s.Lucene
import com.outr.lucene4s.field.Field
import org.apache.lucene.search.ScoreDoc

class SearchResult private[lucene4s](lucene: Lucene, search: PagedResults[_], scoreDoc: ScoreDoc) {
  private lazy val doc = lucene.searcher.doc(scoreDoc.doc)

  def apply[T](field: Field[T]): T = field.support.fromLucene(doc.getField(field.name))
  // TODO: create get[T](field: Field[T]): Option[T]

  def id: Int = scoreDoc.doc
  def score: Double = scoreDoc.score.toDouble
  def shardIndex: Int = scoreDoc.shardIndex

  def highlighting[T](field: Field[T]): List[HighlightedResult] = search.highlighter match {
    case Some(highlighter) => {
      val text = doc.get(field.name)
      val tokenStream = lucene.standardAnalyzer.tokenStream(field.name, text)
      val mergeContiguousFragments = false
      val maxNumFragments = 10
      val fragments = highlighter.getBestTextFragments(tokenStream, text, mergeContiguousFragments, maxNumFragments)
      fragments.toList.collect {
        case frag if frag.getScore > 0.0f => HighlightedResult(frag.toString, frag.getScore.toDouble)
      }
    }
    case None => Nil
  }
}

case class HighlightedResult(content: String, score: Double) {
  lazy val fragment: String = content.split('\n').find(_.indexOf("<em>") != -1).getOrElse(content)
  lazy val word: String = HighlightedResult.WordExtractionRegex.findFirstMatchIn(fragment).map(_.group(1)).getOrElse(fragment)
}

object HighlightedResult {
  private val WordExtractionRegex = """[<]em[>](.+)[<][/]em[>]""".r
}