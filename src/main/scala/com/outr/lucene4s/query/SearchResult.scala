package com.outr.lucene4s.query

import com.outr.lucene4s.Lucene
import com.outr.lucene4s.field.Field
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.highlight.TokenSources

class SearchResult private[lucene4s](lucene: Lucene, search: PagedResults[_], scoreDoc: ScoreDoc) {
  private lazy val doc = lucene.searcher.doc(scoreDoc.doc)

  def apply[T](field: Field[T]): T = field.support.fromLucene(doc.getField(field.name))
  // TODO: create get[T](field: Field[T]): Option[T]

  def id: Int = scoreDoc.doc
  def score: Double = scoreDoc.score.toDouble
  def shardIndex: Int = scoreDoc.shardIndex

  def highlighting[T](field: Field[T]): List[String] = search.highlighter match {
    case Some(highlighter) => {
      val text = doc.get(field.name)
      val tokenStream = lucene.standardAnalyzer.tokenStream(field.name, text)
      val mergeContiguousFragments = false
      val maxNumFragments = 10
      val fragments = highlighter.getBestTextFragments(tokenStream, text, mergeContiguousFragments, maxNumFragments)
      fragments.toList.collect {
        case frag if frag.getScore > 0.0f => {
          val s = frag.toString
          s.split('\n').find(_.indexOf("<em>") != -1).getOrElse(s)
        }
      }
    }
    case None => Nil
  }
}
