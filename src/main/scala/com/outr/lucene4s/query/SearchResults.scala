package com.outr.lucene4s.query

import com.outr.lucene4s.facet.FacetField
import org.apache.lucene.search.TopDocs

case class SearchResults(topDocs: TopDocs, facetResults: Map[FacetField, FacetResult])