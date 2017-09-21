package com.outr.lucene4s.query

import com.outr.lucene4s.Lucene
import com.outr.lucene4s.facet.FacetField
import org.apache.lucene.search.highlight.Highlighter

class PagedResults[T] private[lucene4s](val lucene: Lucene,
                                        val query: QueryBuilder[T],
                                        val offset: Int,
                                        searchResults: SearchResults,
                                        val highlighter: Option[Highlighter]) {
  lazy val results: Vector[SearchResult] = searchResults.topDocs.scoreDocs.toVector.map(sd => new SearchResult(lucene, this, sd))
  lazy val entries: Vector[T] = results.map(query.conversion)

  def apply(index: Int): T = entries(index)

  def pageSize: Int = query.limit
  def total: Long = searchResults.topDocs.totalHits
  def pageIndex: Int = offset / pageSize
  def pages: Int = math.ceil(total.toDouble / pageSize.toDouble).toInt
  def maxScore: Double = searchResults.topDocs.getMaxScore.toDouble

  def facets: Map[FacetField, FacetResult] = searchResults.facetResults
  def facet(field: FacetField): Option[FacetResult] = facets.get(field)

  def page(index: Int): PagedResults[T] = query.offset(pageSize * index).search()
  def hasNextPage: Boolean = ((pageIndex + 1) * pageSize) < total
  def hasPreviousPage: Boolean = offset > 0
  def nextPage(): Option[PagedResults[T]] = if (hasNextPage) {
    Some(page(pageIndex + 1))
  } else {
    None
  }
  def previousPage(): Option[PagedResults[T]] = if (hasPreviousPage) {
    Some(page(pageIndex - 1))
  } else {
    None
  }
}
