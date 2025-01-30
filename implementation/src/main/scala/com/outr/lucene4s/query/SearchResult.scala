package com.outr.lucene4s.query

import com.outr.lucene4s._
import com.outr.lucene4s.document.DocumentBuilder
import com.outr.lucene4s.field.Field
import org.apache.lucene.document.Document
import org.apache.lucene.search.ScoreDoc

class SearchResult private[lucene4s](lucene: Lucene, search: PagedResults[_], scoreDoc: ScoreDoc) {
  private lazy val doc: Document = lucene.withSearcherAndTaxonomy(_.searcher.storedFields.document(scoreDoc.doc))

  def apply[T](field: Field[T]): T = field.support.fromLucene(doc.getFields(field.storeName).toList)

  def id: Int = scoreDoc.doc
  def score: Double = scoreDoc.score.toDouble
  def shardIndex: Int = scoreDoc.shardIndex

  def update: DocumentBuilder = {
    val identifiers = lucene.uniqueFields.map(lucene.field[Any]).map(f => exact(f(apply(f))))
    val query = grouped(identifiers.map(_ -> Condition.Must): _*)
    val builder = new DocumentBuilder(lucene, Some(query), doc)
    builder.rebuildFacetsFromDocument()
    builder
  }

  def highlighting[T](field: Field[T]): List[HighlightedResult] = search.highlighter match {
    case Some(highlighter) => {
      val text = doc.get(field.storeName)
      val tokenStream = lucene.analyzer.tokenStream(field.storeName, text)
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
  lazy val word: String = {
    val start = fragment.indexOf("<em>")
    val end = fragment.indexOf("</em>", start + 4)
    assert(start != -1, s"Unable to find emphasis start tag in fragment ($fragment).")
    assert(end != -1, s"Unable to find emphasis end tag in fragment ($fragment).")
    fragment.substring(start + 4, end)
  }
}