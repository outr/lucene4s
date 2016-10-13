package com.outr.lucene4s.document

import com.outr.lucene4s.Lucene
import com.outr.lucene4s.facet.FacetValue
import com.outr.lucene4s.field.value.FieldAndValue
import org.apache.lucene.document.Document
import org.apache.lucene.index.Term

class DocumentBuilder(lucene: Lucene, update: Option[FieldAndValue[String]], document: Document = new Document) {
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
    update match {
      case Some(u) => {
        lucene.indexWriter.updateDocument(new Term(u.field.name, u.value), doc)
      }
      case None => {
        lucene.indexWriter.addDocument(doc)
      }
    }
  }
}