package com.outr.lucene4s.query

import com.outr.lucene4s.Lucene
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{Sort, TopDocs, TopScoreDocCollector}

case class QueryBuilder private[lucene4s](lucene: Lucene,
                                          defaultField: String,
                                          offset: Int = 0,
                                          limit: Int = 10,
                                          scoreDocs: Boolean = false,
                                          scoreMax: Boolean = false) {
  def offset(v: Int): QueryBuilder = copy(offset = v)
  def limit(v: Int): QueryBuilder = copy(limit = v)

  def scoreDocs(b: Boolean = true): QueryBuilder = copy(scoreDocs = b)

  def scoreMax(b: Boolean = true): QueryBuilder = copy(scoreMax = b)

  def search(query: String = "*:*"): PagedResults = {
    val parser = new QueryParser(defaultField, lucene.standardAnalyzer)
    val q = parser.parse(query)
    val sort = Sort.RELEVANCE   // TODO: support sorting
    // TODO: support really high offsets via multiple jumps via searchAfter to avoid memory issues
    val topDocs = lucene.searcher.search(q, offset + limit, sort, scoreDocs, scoreMax) match {
      case td if offset != 0 => {    // Build a new TopDocs instance excluding the skipped
        new TopDocs(td.totalHits, td.scoreDocs.slice(offset, offset + limit), td.getMaxScore)
      }
      case td => td
    }
    new PagedResults(lucene, this, query, offset, topDocs)
  }
}