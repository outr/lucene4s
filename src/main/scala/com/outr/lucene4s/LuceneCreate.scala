package com.outr.lucene4s

import com.outr.lucene4s.facet.FacetField
import com.outr.lucene4s.field.value.support.ValueSupport
import com.outr.lucene4s.field.{Field, FieldType}
import com.outr.lucene4s.mapper.{BaseSearchable, SearchableMacro}

import scala.language.experimental.macros

class LuceneCreate(val lucene: Lucene) {
  def field[T](name: String,
               fieldType: FieldType = FieldType.Stored,
               fullTextSearchable: Boolean = lucene.defaultFullTextSearchable
              )(implicit support: ValueSupport[T]): Field[T] = {
    new Field[T](name, fieldType, support, fullTextSearchable)
  }
  def facet(name: String,
            hierarchical: Boolean = false,
            multiValued: Boolean = false,
            requireDimCount: Boolean = false): FacetField = {
    lucene.facetsConfig.setHierarchical(name, hierarchical)
    lucene.facetsConfig.setMultiValued(name, multiValued)
    lucene.facetsConfig.setRequireDimCount(name, requireDimCount)
    FacetField(name, hierarchical, multiValued, requireDimCount)
  }
  def searchable[S <: BaseSearchable]: S = macro SearchableMacro.generate[S]
}
