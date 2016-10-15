package com.outr.lucene4s.query

import com.outr.lucene4s._
import com.outr.lucene4s.facet.FacetField
import org.apache.lucene.facet.DrillDownQuery

case class QueryBuilder[T] private[lucene4s](lucene: Lucene,
                                             facets: Set[FacetQuery] = Set.empty,
                                             offset: Int = 0,
                                             limit: Int = 10,
                                             sorting: List[Sort] = Nil,
                                             scoreDocs: Boolean = false,
                                             scoreMax: Boolean = false,
                                             searchTerms: List[SearchTerm] = Nil,
                                             conversion: SearchResult => T) {
  def offset(v: Int): QueryBuilder[T] = copy(offset = v)
  def limit(v: Int): QueryBuilder[T] = copy(limit = v)

  def convert[V](conversion: SearchResult => V): QueryBuilder[V] = copy[V](conversion = conversion)

  def facet(field: FacetField, limit: Int = 10, path: List[String] = Nil): QueryBuilder[T] = copy(facets = facets + FacetQuery(field, limit, path))

  def scoreDocs(b: Boolean = true): QueryBuilder[T] = copy(scoreDocs = b)

  def scoreMax(b: Boolean = true): QueryBuilder[T] = copy(scoreMax = b)

  def filter(searchTerms: SearchTerm*): QueryBuilder[T] = copy(searchTerms = this.searchTerms ::: searchTerms.toList)

  def sort(sort: Sort*): QueryBuilder[T] = {
    copy(sorting = sorting ::: sort.toList)
  }

  def replaceSort(sort: Sort*): QueryBuilder[T] = {
    copy(sorting = sort.toList)
  }

  def search(): PagedResults[T] = {
    val baseQuery = searchTerms match {
      case Nil => MatchAllSearchTerm
      case st :: Nil => st
      case _ => grouped(searchTerms.map(_ -> Condition.Must): _*)
    }
    val q = baseQuery.toLucene(lucene) match {
      case query if facets.exists(_.path.nonEmpty) => {
        val drillDown = new DrillDownQuery(lucene.facetsConfig, query)
        facets.foreach { fq =>
          if (fq.path.nonEmpty) {
            drillDown.add(fq.facet.name, fq.path: _*)
          }
        }
        drillDown
      }
      case parsedQuery => parsedQuery
    }
    // TODO: support really high offsets via multiple jumps via searchAfter to avoid memory issues

    val manager = new DocumentCollector(lucene, this)
    val searchResults = lucene.searcher.search(q, manager)
    new PagedResults(lucene, this, offset, searchResults)
  }
}