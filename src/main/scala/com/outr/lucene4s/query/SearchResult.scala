package com.outr.lucene4s.query

import com.outr.lucene4s.Lucene
import com.outr.lucene4s.field.Field
import org.apache.lucene.search.ScoreDoc

class SearchResult private[lucene4s](lucene: Lucene, search: PagedResults, scoreDoc: ScoreDoc) {
  private lazy val doc = lucene.searcher.doc(scoreDoc.doc)

  def apply[T](field: Field[T]): T = field.support.fromLucene(doc.getField(field.name))
}
