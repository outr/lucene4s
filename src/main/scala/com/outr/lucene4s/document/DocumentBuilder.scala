package com.outr.lucene4s.document

import com.outr.lucene4s._
import com.outr.lucene4s.facet.FacetValue
import com.outr.lucene4s.field.value.FieldAndValue
import com.outr.lucene4s.query.Condition
import org.apache.lucene.document.Document
import org.apache.lucene.index.Term

class DocumentBuilder(lucene: Lucene, update: List[FieldAndValue[_]], document: Document = new Document) {
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
    val doc = lucene.facetsConfig.build(lucene.taxonomyWriter, document)
    if (update.nonEmpty) {              // We need to do an update, so delete based on the criteria
      val terms = update.map { fv =>
        term(fv) -> Condition.Must
      }
      lucene.delete(grouped(terms: _*))
    }
    lucene.indexWriter.addDocument(doc)
  }
}