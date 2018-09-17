package com.outr.lucene4s.document

import com.outr.lucene4s._
import com.outr.lucene4s.facet.{FacetField, FacetValue}
import com.outr.lucene4s.field.Field
import com.outr.lucene4s.field.value.FieldAndValue
import com.outr.lucene4s.query.SearchTerm
import org.apache.lucene.document.Document

import scala.collection.mutable.ListBuffer

class DocumentBuilder(lucene: Lucene,
                      val update: Option[SearchTerm],
                      val document: Document = new Document) {
  var fullText: List[String] = List.empty[String]

  private val _values = ListBuffer.empty[FieldAndValue[_]]
  private val _facetValues = ListBuffer.empty[FacetValue]

  def values: List[FieldAndValue[_]] = _values.toList
  def valueForField[T](field: Field[T]): Option[FieldAndValue[T]] = {
    values.find(_.field.name == field.name).asInstanceOf[Option[FieldAndValue[T]]]
  }
  def valueForName(name: String): Option[FieldAndValue[_]] = values.find(_.field.name == name)

  def facetValues: List[FacetValue] = _facetValues.toList
  def facetsForField(field: FacetField): List[FacetValue] = facetValues.filter(_.field.name == field.name)
  def facetsForName(name: String): List[FacetValue] = facetValues.filter(_.field.name == name)

  private[lucene4s] def rebuildFacetsFromDocument(): Unit = {
    lucene.facets.foreach { ff =>
      val field = document.getField(ff.name)
      val path = field.stringValue().split('/').toList
      facets(ff(path: _*))
    }
  }

  def fields(fieldAndValues: FieldAndValue[_]*): DocumentBuilder = synchronized {
    fieldAndValues.foreach { fv =>
      document.removeFields(fv.field.name)      // Remove existing by name
      fv.write(document)
      if (fv.field.fullTextSearchable) {
        fullText = fv.value.toString :: fullText
      }
      _values += fv
    }
    this
  }

  def facets(facetValues: FacetValue*): DocumentBuilder = synchronized {
    facetValues.foreach { fv =>
      document.removeFields(fv.field.name)
      fv.write(document)
      _facetValues += fv
    }
    this
  }

  def index(): Unit = lucene.index(this)
}