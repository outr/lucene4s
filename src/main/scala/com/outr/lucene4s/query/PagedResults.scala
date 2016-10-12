package com.outr.lucene4s.query

import com.outr.lucene4s.Lucene
import com.outr.lucene4s.facet.FacetField

class PagedResults private[lucene4s](val lucene: Lucene,
                                     val query: QueryBuilder,
                                     val queryString: String,
                                     val offset: Int,
                                     searchResults: SearchResults) {
  lazy val results: Vector[SearchResult] = searchResults.topDocs.scoreDocs.toVector.map(sd => new SearchResult(lucene, this, sd))

  def pageSize: Int = query.limit
  def total: Int = searchResults.topDocs.totalHits
  def pageIndex: Int = offset / pageSize
  def pages: Int = math.ceil(total.toDouble / pageSize.toDouble).toInt
  def maxScore: Double = searchResults.topDocs.getMaxScore.toDouble

  def facet(field: FacetField): Option[FacetResult] = searchResults.facetResults.get(field)

  def page(index: Int): PagedResults = query.offset(pageSize * index).search(queryString)
  def hasNextPage: Boolean = ((pageIndex + 1) * pageSize) < total
  def hasPreviousPage: Boolean = offset > 0
  def nextPage(): Option[PagedResults] = if (hasNextPage) {
    Some(page(pageIndex + 1))
  } else {
    None
  }
  def previousPage(): Option[PagedResults] = if (hasPreviousPage) {
    Some(page(pageIndex - 1))
  } else {
    None
  }
}
