package com.outr.lucene4s.query

import com.outr.lucene4s.Lucene
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.Sort

case class QueryBuilder private[lucene4s](lucene: Lucene,
                                          defaultField: String,
                                          limit: Int = 10,
                                          scoreDocs: Boolean = false,
                                          scoreMax: Boolean = false) {
  def limit(v: Int): QueryBuilder = copy(limit = v)

  def scoreDocs(b: Boolean = true): QueryBuilder = copy(scoreDocs = b)

  def scoreMax(b: Boolean = true): QueryBuilder = copy(scoreMax = b)

  def search(query: String): SearchResults = {
    val parser = new QueryParser(defaultField, lucene.standardAnalyzer)
    val q = parser.parse(query)
    val sort = Sort.INDEXORDER
    val topDocs = lucene.searcher.search(q, limit, sort, scoreDocs, scoreMax)
    new SearchResults(lucene, topDocs)
  }
}
