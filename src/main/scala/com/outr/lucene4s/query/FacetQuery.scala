package com.outr.lucene4s.query

import com.outr.lucene4s.facet.FacetField

case class FacetQuery(facet: FacetField, limit: Int, path: List[String])
