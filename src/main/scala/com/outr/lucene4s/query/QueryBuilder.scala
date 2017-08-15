package com.outr.lucene4s.query

import com.outr.lucene4s._
import com.outr.lucene4s.facet.FacetField
import org.apache.lucene.facet.DrillDownQuery
import org.apache.lucene.search.BooleanClause.Occur
import org.apache.lucene.search.{BooleanQuery, BoostQuery, TermQuery}
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
                                             highlighting: Option[Highlighting] = None,
                                             minimumShouldMatch: Int = 0) {
  def offset(v: Int): QueryBuilder[T] = copy(offset = v)
  def limit(v: Int): QueryBuilder[T] = copy(limit = v)
  def minimumShouldMatch(v: Int): QueryBuilder[T] = copy(minimumShouldMatch = v)

  def convert[V](conversion: SearchResult => V): QueryBuilder[V] = copy[V](conversion = conversion)

  def facet(field: FacetField,
            limit: Int = 10,
            path: List[String] = Nil,
            condition: Condition = Condition.Must,
            onlyThisLevel: Boolean = false): QueryBuilder[T] = {
    copy(facets = facets + FacetQuery(field, limit, path, condition, onlyThisLevel))
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
    val qb = new BooleanQuery.Builder
    qb.setMinimumNumberShouldMatch(minimumShouldMatch)
    qb.add(baseQuery.toLucene(lucene), Occur.MUST)
    facets.foreach { fq =>
      if (fq.path.nonEmpty || fq.onlyThisLevel) {
        val indexedField = lucene.facetsConfig.getDimConfig(fq.facet.name).indexFieldName
        val path = if (fq.onlyThisLevel) {
          fq.path ::: List("$ROOT$")
        } else {
          fq.path
        }
        qb.add(new BoostQuery(new TermQuery(DrillDownQuery.term(indexedField, fq.facet.name, path: _*)), 0.0f), fq.condition.occur)
      }
    }
    val q = qb.build()
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