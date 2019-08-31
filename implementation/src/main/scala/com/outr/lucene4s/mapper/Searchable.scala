package com.outr.lucene4s.mapper

import com.outr.lucene4s._
import com.outr.lucene4s.document.DocumentBuilder
import com.outr.lucene4s.field.Field
import com.outr.lucene4s.query.{Condition, QueryBuilder, SearchResult, SearchTerm}

import scala.annotation.compileTimeOnly
import scala.reflect.macros.blackbox

sealed trait BaseSearchable

trait Searchable[T] extends BaseSearchable {
  def lucene: Lucene

  protected def idAndDocSearchTerm(t: T): SearchTerm = {
    val terms = idSearchTerms(t) ::: List(exact(docType(docTypeName)))
    grouped(terms.map(_ -> Condition.Must): _*)
  }

  /**
    * The document type name. Automatically generated if not defined.
    */
  val docTypeName: String

  val docType: Field[String] = lucene.create.field[String]("docType", fullTextSearchable = false)

  /**
    * Creates a query that filters to only this document type and automatically supports conversion to T.
    */
  def query(): QueryBuilder[T] = lucene.query().filter(term(docType(docTypeName.toLowerCase))).convert[T](apply)

  /**
    * Generates a list of search terms to find the supplied value. This is used by update and delete to make sure the
    * right value is replaced or removed.
    */
  def idSearchTerms(t: T): List[SearchTerm]

  /**
    * Converts the SearchResult into and instance of T
    *
    * @param result the search result
    * @return T
    */
  def apply(result: SearchResult): T = throw new UnsupportedOperationException("Should be supplied by Macro!")

  /**
    * Creates a DocumentBuilder to insert `value` into Lucene.
    *
    * @param value the value to insert
    */
  def insert(value: T): DocumentBuilder = throw new UnsupportedOperationException("Should be supplied by Macro!")

  /**
    * Creates a DocumentBuilder to update `value` into Lucene.
    *
    * @param value the updated value to replace what is currently indexed
    */
  def update(value: T): DocumentBuilder = throw new UnsupportedOperationException("Should be supplied by Macro!")

  /**
    * Deletes the value from the index.
    *
    * @param value the value to delete
    */
  def delete(value: T): Unit = throw new UnsupportedOperationException("Should be supplied by Macro!")
}

@compileTimeOnly("Enable macro paradise to expand macro annotations")
object SearchableMacro {
  def generate[S <: BaseSearchable](c: blackbox.Context)(implicit s: c.WeakTypeTag[S]): c.Expr[S] = {
    import c.universe._

    val luceneCreate = c.prefix.tree
    val t = s.tpe.baseType(symbolOf[Searchable[_]]).typeArgs.head
    val members = t.decls
    val fields = members.filter(s => s.asTerm.isVal && s.asTerm.isCaseAccessor)
    val fieldNames = fields.map(_.name.decodedName.toString.trim)
    val fieldTypes = fields.map(_.info)

    val values = s.tpe.decls.collect {
      case symbol if symbol.toString.startsWith("value ") => symbol.name.toString.trim
    }.toSet
    val docTypeName = if (values.contains("docTypeName")) {
      q""
    } else {
      q"override val docTypeName: String = ${t.typeSymbol.fullName.replaceAllLiterally(".", "").replaceAllLiterally("$", "")}"
    }

    val fieldInstances = fieldNames.zip(fieldTypes).collect {
      case (n, t) if !values.contains(n) => {
        q"val ${TermName(n)} = lucene.create.field[$t]($n, fullTextSearchable = true)"
      }
    }
    val searchFields = fieldNames.map { n =>
      val tn = TermName(n)
      q"$tn(value.$tn)"
    }
    val fromDocument = fieldNames.map { n =>
      val tn = TermName(n)
      q"$tn = result($tn)"
    }
    val result2T = q"new $t(..$fromDocument)"
    val insertDocument = q"lucene.doc().fields(..$searchFields).fields(docType(docTypeName))"
    val updateDocument = q"lucene.update(idAndDocSearchTerm(value)).fields(..$searchFields).fields(docType(docTypeName))"
    val deleteDocument = q"lucene.delete(idAndDocSearchTerm(value))"

    c.Expr[S](
      q"""
         new $s {
            override def lucene = $luceneCreate.lucene

            $docTypeName

            override def apply(result: com.outr.lucene4s.query.SearchResult) = $result2T

            override def insert(value: $t) = $insertDocument

            override def update(value: $t) = $updateDocument

            override def delete(value: $t) = $deleteDocument

            ..$fieldInstances
         }
       """)
  }
}