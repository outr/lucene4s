package com.outr.lucene4s.facet

case class FacetField(name: String, hierarchical: Boolean, multiValued: Boolean, requireDimCount: Boolean) {
  def apply(values: String*): FacetValue = new FacetValue(this, values: _*)
}