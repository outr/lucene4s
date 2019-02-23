package com.outr.lucene4s.query

import org.apache.lucene.search.{LeafCollector, Scorer}

class CollectorsLeafCollector(leafCollectors: List[LeafCollector]) extends LeafCollector {
  override def setScorer(scorer: Scorer): Unit = {
    leafCollectors.foreach { leafCollector =>
      leafCollector.setScorer(scorer)
    }
  }

  override def collect(doc: Int): Unit = {
    leafCollectors.foreach { leafCollector =>
      leafCollector.collect(doc)
    }
  }
}
