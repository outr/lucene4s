package com.outr.lucene4s.query

import java.util

import com.outr.lucene4s.Lucene
import org.apache.lucene.facet.FacetsCollector
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
      case Collectors(topFieldCollector, facetsCollector) => topFieldCollector.topDocs()
    }.toArray

    val docMax = math.max(1, lucene.indexReader.maxDoc())
    val docLimit = math.min(query.offset + query.limit, docMax)

    val topFieldDocs = TopDocs.merge(sort, docLimit, topDocs) match {
      case td if query.offset > 0 => {
        new TopDocs(td.totalHits, td.scoreDocs.slice(query.offset, query.offset + query.limit), td.getMaxScore)
      }
      case td => td
    }
    SearchResults(topFieldDocs)
  }
}
