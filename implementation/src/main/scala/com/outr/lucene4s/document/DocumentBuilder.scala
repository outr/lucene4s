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

  def values: List[FieldAndValue[_]] = _values.toList
  def valueForField[T](field: Field[T]): Option[FieldAndValue[T]] = {
    values.find(_.field.name == field.name).asInstanceOf[Option[FieldAndValue[T]]]
  }
  def valueForName(name: String): Option[FieldAndValue[_]] = values.find(_.field.name == name)

  private var unwrittenFacets = Set.empty[FacetValue]

  private[lucene4s] def rebuildFacetsFromDocument(): Unit = {
    lucene.facets.foreach { ff =>
      document.getFields(ff.name).foreach { field =>
        val path = field.stringValue().split('/').toList
        unwrittenFacets += ff(path: _*)
      }
    }
  }

  def fields(fieldAndValues: FieldAndValue[_]*): DocumentBuilder = synchronized {
    fieldAndValues.foreach { fv =>
      fv.write(document)
      if (fv.field.fullTextSearchable) {
        fullText = fv.value.toString :: fullText
      }
      _values += fv
    }
    this
  }

  def clear[T](field: Field[T]): DocumentBuilder = clear(field.name)

  def clear(fieldName: String): DocumentBuilder = {
    document.removeFields(fieldName)
    unwrittenFacets = unwrittenFacets.filterNot(_.field.name == fieldName)
    this
  }

  def remove[T](fv: FacetValue): DocumentBuilder = {
    val values = document.getFields(fv.field.name).toList.map(_.stringValue()).distinct
    val updated = values.collect {
      case v if v != fv.pathString => new FacetValue(fv.field, v.split('/'): _*)
    }
    clear(fv.field.name)
    facets(updated: _*)
  }

  def facets(facetValues: FacetValue*): DocumentBuilder = synchronized {
    unwrittenFacets ++= facetValues.toSet
    this
  }

  def prepareForWriting(): Unit = {
    // Write facets
    unwrittenFacets.foreach(fv => fv.write(document))
    unwrittenFacets = Set.empty
  }

  def index(): Unit = lucene.index(this)
}