package com.outr.lucene4s.query

import org.apache.lucene.facet.FacetsCollector
import org.apache.lucene.index.LeafReaderContext
import org.apache.lucene.search.{CollectionTerminatedException, Collector, LeafCollector, TopFieldCollector}

case class Collectors(topFieldCollector: TopFieldCollector, facetsCollector: FacetsCollector) extends Collector {
  override def getLeafCollector(context: LeafReaderContext): LeafCollector = {
    val topFieldLeaf = try {
      Some(topFieldCollector.getLeafCollector(context))
    } catch {
      case exc: CollectionTerminatedException => None
    }
    val facetsLeaf = try {
      Some(facetsCollector.getLeafCollector(context))
    } catch {
      case exc: CollectionTerminatedException => None
    }
    val leafCollectors = List(topFieldLeaf, facetsLeaf).flatten
    leafCollectors match {
      case Nil => throw new CollectionTerminatedException()
      case leafCollector :: Nil => leafCollector
      case _ => new CollectorsLeafCollector(leafCollectors)
    }
  }

  override def needsScores(): Boolean = topFieldCollector.needsScores() || facetsCollector.needsScores()
}
