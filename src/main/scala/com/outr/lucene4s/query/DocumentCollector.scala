package com.outr.lucene4s.query

import java.util

import com.outr.lucene4s.Lucene
import com.outr.lucene4s.facet.{FacetField, FacetValue}
import org.apache.lucene.facet.FacetsCollector
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts
import org.apache.lucene.search.{CollectorManager, Sort, TopDocs, TopFieldCollector}

import scala.collection.JavaConversions._

class DocumentCollector(lucene: Lucene, query: QueryBuilder) extends CollectorManager[Collectors, SearchResults] {
  val sort = Sort.RELEVANCE   // TODO: support sorting

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
        val r = facets.getTopChildren(fq.limit, fq.facet.name, fq.path: _*)
        val values = r.labelValues.toVector.map(lv => FacetResultValue(lv.label, lv.value.intValue()))
        val facetResult = FacetResult(fq.facet, values, r.childCount, r.value.intValue())
        facetResults += fq.facet -> facetResult
      }
    }

    SearchResults(topFieldDocs, facetResults)
  }
}

case class FacetResult(field: FacetField, values: Vector[FacetResultValue], childCount: Int, count: Int)

case class FacetResultValue(value: String, count: Int)