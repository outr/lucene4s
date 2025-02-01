package com.outr.lucene4s.query

import org.apache.lucene.search.{LeafCollector, Scorable, Scorer}

class CollectorsLeafCollector(leafCollectors: List[LeafCollector]) extends LeafCollector {
  override def setScorer(scorer: Scorable): Unit = {
    leafCollectors.foreach { leafCollector =>
      leafCollector.setScorer(scorer)
    }
  }

  override def collect(doc: Int): Unit = {
    leafCollectors.foreach { leafCollector =>
      leafCollector.collect(doc)
    }
  }

  override def finish(): Unit =
    leafCollectors.foreach(_.finish())
}
