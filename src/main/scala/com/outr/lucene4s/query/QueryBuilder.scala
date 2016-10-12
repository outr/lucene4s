package com.outr.lucene4s.query

import com.outr.lucene4s.Lucene
import com.outr.lucene4s.facet.FacetField
import org.apache.lucene.queryparser.classic.QueryParser

case class QueryBuilder private[lucene4s](lucene: Lucene,
                                          defaultField: String,
                                          facets: Set[FacetQuery] = Set.empty,
                                          offset: Int = 0,
                                          limit: Int = 10,
                                          scoreDocs: Boolean = false,
                                          scoreMax: Boolean = false) {
  def offset(v: Int): QueryBuilder = copy(offset = v)
  def limit(v: Int): QueryBuilder = copy(limit = v)

  def facet(field: FacetField, limit: Int = 10, path: List[String] = Nil): QueryBuilder = copy(facets = facets + FacetQuery(field, limit, path))

  def scoreDocs(b: Boolean = true): QueryBuilder = copy(scoreDocs = b)

  def scoreMax(b: Boolean = true): QueryBuilder = copy(scoreMax = b)

  def search(query: String = "*:*"): PagedResults = {
    val parser = new QueryParser(defaultField, lucene.standardAnalyzer)
    val q = parser.parse(query)
    // TODO: support really high offsets via multiple jumps via searchAfter to avoid memory issues

    val manager = new DocumentCollector(lucene, this)
    val searchResults = lucene.searcher.search(q, manager)
    new PagedResults(lucene, this, query, offset, searchResults)
  }
}