package com.outr.lucene4s.query

import com.outr.lucene4s.Lucene
import org.apache.lucene.search.TopDocs

class SearchResults private[lucene4s](lucene: Lucene, topDocs: TopDocs) {
  lazy val results: Vector[SearchResult] = topDocs.scoreDocs.toVector.map(sd => new SearchResult(lucene, this, sd))
}
