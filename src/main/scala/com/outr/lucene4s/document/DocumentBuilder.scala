package com.outr.lucene4s.document

import com.outr.lucene4s._
import com.outr.lucene4s.facet.FacetValue
import com.outr.lucene4s.field.Field
import com.outr.lucene4s.field.value.FieldAndValue
import com.outr.lucene4s.query.SearchTerm
import org.apache.lucene.document.Document
import org.apache.lucene.index.Term

import scala.collection.mutable.ListBuffer

class DocumentBuilder(lucene: Lucene,
                      update: Option[SearchTerm],
                      val document: Document = new Document) {
  var fullText: List[String] = List.empty[String]

  private val _values = ListBuffer.empty[FieldAndValue[_]]
  def values: List[FieldAndValue[_]] = _values.toList
  def valueForField[T](field: Field[T]): Option[FieldAndValue[T]] = {
    values.find(_.field.name == field.name).asInstanceOf[Option[FieldAndValue[T]]]
  }
  def valueForName(name: String): Option[FieldAndValue[_]] = values.find(_.field.name == name)

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

  def facets(facetValues: FacetValue*): DocumentBuilder = {
    facetValues.foreach(_.write(document))
    this
  }

  def index(): Unit = {
    if (fullText.nonEmpty) {
      val fullTextString: String = fullText.mkString("\n")
      fields(lucene.fullText(fullTextString))
    }
    val doc = lucene.facetsConfig.build(lucene.taxonomyWriter, document)

    update.foreach { searchTerm =>
      // We need to do an update, so delete based on the criteria
      lucene.delete(searchTerm)
    }
    lucene.indexWriter.addDocument(doc)
    lucene.indexed(this)
  }
}