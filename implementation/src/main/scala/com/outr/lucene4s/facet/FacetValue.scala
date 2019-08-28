package com.outr.lucene4s.facet

import com.outr.lucene4s.field.FieldType
import org.apache.lucene.document.{Document, Field}
import org.apache.lucene.facet.{FacetField => LuceneFacetField}

class FacetValue(val field: FacetField, val path: String*) {
  lazy val pathString: String = path.mkString("/")

  private[lucene4s] def write(document: Document): Unit = {
    val updatedPath = if (field.hierarchical) path.toList ::: List("$ROOT$") else path
    document.add(new Field(field.name, pathString, FieldType.Stored.lucene()))
    document.add(new LuceneFacetField(field.name, updatedPath: _*))
  }
}