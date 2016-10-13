package com.outr.lucene4s.query

import java.util

import com.outr.lucene4s.Lucene
import com.outr.lucene4s.facet.{FacetField, FacetValue}
import org.apache.lucene.facet.FacetsCollector
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts
import org.apache.lucene.search.{CollectorManager, Sort => LuceneSort, TopDocs, TopFieldCollector}

import scala.collection.JavaConversions._

class DocumentCollector(lucene: Lucene, query: QueryBuilder) extends CollectorManager[Collectors, SearchResults] {
  val sort = if (query.sorting.nonEmpty) {
    new LuceneSort(query.sorting.map(_.sortField()): _*)
  } else {
    LuceneSort.RELEVANCE
  }

  override def newCollector(): Collectors = {
    val docMax = math.max(1, lucene.indexReader.maxDoc())
    val docLimit = math.min(query.offset + query.limit, docMax)

    val fillFields = true
    val topFieldCollector = TopFieldCollector.create(sort, docLimit, null, fillFields, query.scoreDocs, query.scoreMax)
    val facetsCollector = new FacetsCollector(query.scoreDocs)
    Collectors(topFieldCollector, facetsCollector)
  }

  override def reduce(collectors: util.Collection[Collectors]): SearchResults = {
    val topDocs = collectors.collect {
      case Collectors(tfc, _) => tfc.topDocs()
    }.toArray
    val facetsCollector = collectors.head.facetsCollector

    val docMax = math.max(1, lucene.indexReader.maxDoc())
    val docLimit = math.min(query.offset + query.limit, docMax)

    val topFieldDocs = TopDocs.merge(sort, docLimit, topDocs) match {
      case td if query.offset > 0 => {
        new TopDocs(td.totalHits, td.scoreDocs.slice(query.offset, query.offset + query.limit), td.getMaxScore)
      }
      case td => td
    }

    var facetResults = Map.empty[FacetField, FacetResult]
    if (query.facets.nonEmpty) {
      val facets = new FastTaxonomyFacetCounts(lucene.taxonomyReader, lucene.facetsConfig, facetsCollector)
      query.facets.foreach { fq =>
        Option(facets.getTopChildren(fq.limit, fq.facet.name, fq.path: _*)) match {
          case Some(r) => {
            val values = if (r.childCount > 0) r.labelValues.toVector.map(lv => FacetResultValue(lv.label, lv.value.intValue())) else Vector.empty
            val totalCount = values.map(_.count).sum
            val facetResult = FacetResult(fq.facet, values, r.childCount, totalCount)
            facetResults += fq.facet -> facetResult
          }
          case None => facetResults += fq.facet -> FacetResult(fq.facet, Vector.empty, 0, 0)
        }
      }
    }

    SearchResults(topFieldDocs, facetResults)
  }
}