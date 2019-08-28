package com.outr.lucene4s

import com.outr.lucene4s.facet.FacetField
import com.outr.lucene4s.field.value.FieldAndValue
import com.outr.lucene4s.field.value.support.{StringValueSupport, ValueSupport}
import com.outr.lucene4s.field.{Field, FieldType}
import com.outr.lucene4s.mapper.{BaseSearchable, SearchableMacro}
import com.outr.lucene4s.query.SearchTerm
import org.apache.lucene.document.Document
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField

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
    field[T](name, fieldType, fullTextSearchable, sortable)(new ValueSupport[T] {
      override def store(field: Field[T], value: T, document: Document): Unit = {
        StringValueSupport.store(field.asInstanceOf[Field[String]], stringify.toString(value), document)
      }

      override def filter(field: Field[T], value: T, document: Document): Unit = {
        StringValueSupport.filter(field.asInstanceOf[Field[String]], stringify.toString(value), document)
      }

      override def sorted(field: Field[T], value: T, document: Document): Unit = {
        StringValueSupport.sorted(field.asInstanceOf[Field[String]], stringify.toString(value), document)
      }

      override def fromLucene(fields: List[IndexableField]): T = {
        stringify.fromString(StringValueSupport.fromLucene(fields))
      }

      override def sortFieldType: SortField.Type = StringValueSupport.sortFieldType

      override def searchTerm(fv: FieldAndValue[T]): SearchTerm = {
        StringValueSupport.searchTerm(FieldAndValue(fv.field.asInstanceOf[Field[String]], stringify.toString(fv.value)))
      }
    })
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