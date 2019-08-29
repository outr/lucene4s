package com.outr.lucene4s

import com.outr.lucene4s.facet.FacetField
import com.outr.lucene4s.field.value.support.{StringifyValueSupport, ValueSupport}
import com.outr.lucene4s.field.{Field, FieldType}
import com.outr.lucene4s.mapper.{BaseSearchable, SearchableMacro}

import scala.language.experimental.macros

class LuceneCreate(val lucene: Lucene) {
  def field[T](name: String,
               fieldType: FieldType = FieldType.Stored,
               fullTextSearchable: Boolean = lucene.defaultFullTextSearchable,
               sortable: Boolean = true
              )(implicit support: ValueSupport[T]): Field[T] = {
    val field = new Field[T](name, fieldType, support, fullTextSearchable, sortable)
    lucene.synchronized {
      lucene._fields += field
    }
    field
  }
  def stringifiedField[T](name: String,
                          fieldType: FieldType = FieldType.Stored,
                          fullTextSearchable: Boolean = lucene.defaultFullTextSearchable,
                          sortable: Boolean = true)(implicit stringify: Stringify[T]): Field[T] = {
    field[T](name, fieldType, fullTextSearchable, sortable)(StringifyValueSupport(stringify))
  }
  def facet(name: String,
            hierarchical: Boolean = false,
            multiValued: Boolean = false,
            requireDimCount: Boolean = false): FacetField = {
    lucene.facetsConfig.setHierarchical(name, hierarchical)
    lucene.facetsConfig.setMultiValued(name, multiValued)
    lucene.facetsConfig.setRequireDimCount(name, requireDimCount)
    val field = FacetField(name, hierarchical, multiValued, requireDimCount)
    lucene.synchronized {
      lucene._facets += field
    }
    field
  }
  def searchable[S <: BaseSearchable]: S = macro SearchableMacro.generate[S]
}