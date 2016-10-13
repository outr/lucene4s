package com.outr.lucene4s.document

import com.outr.lucene4s.Lucene
import com.outr.lucene4s.facet.FacetValue
import com.outr.lucene4s.field.value.FieldAndValue
import org.apache.lucene.document.Document

class DocumentBuilder(lucene: Lucene, document: Document = new Document) {
  private var fullText = List.empty[String]

  def fields(fieldAndValues: FieldAndValue[_]*): DocumentBuilder = {
    fieldAndValues.foreach { fv =>
      fv.write(document)
      if (fv.field.fullTextSearchable) {
        synchronized {
          fullText = fv.value.toString :: fullText
        }
      }
    }
    this
  }

  def facets(facetValues: FacetValue*): DocumentBuilder = {
    facetValues.foreach(_.write(document))
    this
  }

  def index(): Unit = {
    if (fullText.nonEmpty) {
      val fullTextString = fullText.mkString(" ")
      fields(lucene.fullText(fullTextString))
    }
    lucene.store(document)
  }
}