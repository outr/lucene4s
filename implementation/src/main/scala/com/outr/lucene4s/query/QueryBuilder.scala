package com.outr.lucene4s.query

import com.outr.lucene4s._
import com.outr.lucene4s.facet.FacetField
import org.apache.lucene.search.highlight.{Highlighter, QueryScorer, SimpleHTMLFormatter}

case class QueryBuilder[T] private[lucene4s](lucene: Lucene,
                                             facets: Set[FacetQuery] = Set.empty,
                                             offset: Int = 0,
                                             limit: Int = 10,
                                             sorting: List[Sort] = Nil,
                                             scoreDocs: Boolean = false,
                                             scoreMax: Boolean = false,
                                             searchTerms: List[SearchTerm] = Nil,
                                             conversion: SearchResult => T,
                                             highlighting: Option[Highlighting] = None) {
  def offset(v: Int): QueryBuilder[T] = copy(offset = v)
  def limit(v: Int): QueryBuilder[T] = copy(limit = v)

  def convert[V](conversion: SearchResult => V): QueryBuilder[V] = copy[V](conversion = conversion)

  def facet(field: FacetField,
            limit: Int = 10,
            path: List[String] = Nil): QueryBuilder[T] = {
    copy(facets = facets + FacetQuery(field, limit, path))
  }

  def scoreDocs(b: Boolean = true): QueryBuilder[T] = copy(scoreDocs = b)

  def scoreMax(b: Boolean = true): QueryBuilder[T] = copy(scoreMax = b)

  def filter(searchTerms: SearchTerm*): QueryBuilder[T] = copy(searchTerms = this.searchTerms ::: searchTerms.toList)

  def highlight(preTag: String = "<em>", postTag: String = "</em>"): QueryBuilder[T] = copy(highlighting = Some(Highlighting(preTag, postTag)))

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
    val q = baseQuery.toLucene(lucene)
    // TODO: support really high offsets via multiple jumps via searchAfter to avoid memory issues

    val manager = new DocumentCollector(lucene, this)
    lucene.withSearcherAndTaxonomy { instance =>
      val searchResults = instance.searcher.search(q, manager)
      val highlighter = highlighting.map {
        case Highlighting(preTag, postTag) => {
          val highlightFormatter = new SimpleHTMLFormatter(preTag, postTag)
          new Highlighter(highlightFormatter, new QueryScorer(q))
        }
      }
      new PagedResults(lucene, this, offset, searchResults, highlighter)
    }
  }
}

case class Highlighting(preTag: String, postTag: String)