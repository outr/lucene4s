package com.outr.lucene4s.mapper

import com.outr.lucene4s.Lucene
import com.outr.lucene4s.field.Field
import com.outr.lucene4s.query.SearchResult
import com.outr.scribe.Logging

import scala.annotation.compileTimeOnly
import scala.reflect.macros.blackbox

sealed trait BaseSearchable

trait Searchable[T] extends BaseSearchable {
  def lucene: Lucene

  /**
    * The unique id field. This is used for inserting, updating, and deleting.
    *
    * @return
    */
  def idFields: List[Field[_]]

  /**
    * Converts the SearchResult into and instance of T
    *
    * @param result the search result
    * @return T
    */
  def apply(result: SearchResult): T

  /**
    * Inserts a new document built from T
    *
    * @param value the value to insert
    */
  def insert(value: T): Unit

  /**
    * Replaces the current instance of this value with the supplied one.
    *
    * @param value the updated value to replace what is currently indexed
    */
  def update(value: T): Unit

  /**
    * Deletes the value from the index.
    *
    * @param value the value to delete
    */
  def delete(value: T): Unit
}

@compileTimeOnly("Enable macro paradise to expand macro annotations")
object SearchableMacro extends Logging {
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

    val fieldInstances = fieldNames.zip(fieldTypes).collect {
      case (n, t) if !values.contains(n) => {
        q"val ${TermName(n)} = lucene.create.field[$t]($n, fullTextSearchable = true)"
      }
    }
    val searchFields = fieldNames.map { n =>
      val tn = TermName(n)
      q"$tn(value.$tn)"
    }
    val idFieldValues =
      q"""List(..$searchFields).collect {
            case fv if idFields.map(_.name).contains(fv.field.name) => fv.value match {
              case s: String => fv.copy(fv.field, s.toLowerCase)
              case _ => fv
            }
          }
       """
    val fromDocument = fieldNames.map { n =>
      val tn = TermName(n)
      q"$tn = result($tn)"
    }
    val result2T = q"new $t(..$fromDocument)"
    val insertDocument = q"lucene.doc().fields(..$searchFields).index()"
    val updateDocument = q"lucene.update($idFieldValues: _*).fields(..$searchFields).index()"
    val deleteDocument = q"lucene.delete(com.outr.lucene4s.grouped($idFieldValues.map(com.outr.lucene4s.term).map(_ -> com.outr.lucene4s.query.Condition.Must): _*))"

    c.Expr[S](
      q"""
         new $s {
            override def lucene = $luceneCreate.lucene

            override def apply(result: com.outr.lucene4s.query.SearchResult) = $result2T

            override def insert(value: $t) = $insertDocument

            override def update(value: $t) = $updateDocument

            override def delete(value: $t) = $deleteDocument

            ..$fieldInstances
         }
       """)
  }
}