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
    values.find(_.field.storeName == field.storeName).asInstanceOf[Option[FieldAndValue[T]]]
  }
  def valueForName(name: String): Option[FieldAndValue[_]] = values.find(_.field.storeName == name)

  private var unwrittenFacets = Set.empty[FacetValue]

  private[lucene4s] def rebuildFacetsFromDocument(): Unit = {
    lucene.facets.foreach { ff =>
      document.getFields(ff.name).foreach { field =>
        val s = field.stringValue()
        val path = if (ff.hierarchical) s.split('/').toList else List(s)
        unwrittenFacets += ff(path: _*)
      }
    }
  }

  def update(searchTerm: SearchTerm): DocumentBuilder = new DocumentBuilder(lucene, Some(searchTerm), document)

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

  def clear[T](field: Field[T]): DocumentBuilder = clear(field.storeName).clear(field.filterName).clear(field.sortName)

  def clear(fieldName: String): DocumentBuilder = {
    document.removeFields(fieldName)
    unwrittenFacets = unwrittenFacets.filterNot(_.field.name == fieldName)
    this
  }

  def remove[T](facetValues: FacetValue*): DocumentBuilder = {
    val field = facetValues.head.field
    val excludePaths = facetValues.map(_.pathString).toSet
    val values = document.getFields(field.name).toList.map(_.stringValue()).distinct
    val updated = values.collect {
      case v if !excludePaths.contains(v) => {
        val path = if (field.hierarchical) v.split('/').toList else List(v)
        new FacetValue(field, path: _*)
      }
    }
    clear(field.name)
    facets(updated: _*)
  }

  def facets(facetValues: FacetValue*): DocumentBuilder = synchronized {
    unwrittenFacets ++= facetValues.toSet
    this
  }

  def prepareForWriting(): Unit = {
    // Write facets
    unwrittenFacets.foreach { fv =>
      try {
        if (fv.pathString.trim.nonEmpty || fv.field.hierarchical) {
          fv.write(document)
        }
      } catch {
        case t: Throwable => throw new RuntimeException(s"Failure to write facet: $fv", t)
      }
    }
    unwrittenFacets = Set.empty
  }

  def index(): Unit = lucene.index(this)
}