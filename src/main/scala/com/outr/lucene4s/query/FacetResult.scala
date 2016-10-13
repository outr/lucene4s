package com.outr.lucene4s.query

import com.outr.lucene4s.facet.FacetField

case class FacetResult(field: FacetField, values: Vector[FacetResultValue], childCount: Int, totalCount: Int)
