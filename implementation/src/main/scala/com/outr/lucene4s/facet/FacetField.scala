package com.outr.lucene4s.facet

case class FacetField(name: String, hierarchical: Boolean, multiValued: Boolean, requireDimCount: Boolean) {
  def apply(path: String*): FacetValue = new FacetValue(this, path: _*)
}