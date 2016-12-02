package com.outr.lucene4s

import java.nio.file.Path

import akka.actor.ActorSystem
import com.outr.lucene4s.document.DocumentBuilder
import com.outr.lucene4s.facet.FacetField
import com.outr.lucene4s.field.value.support.ValueSupport
import com.outr.lucene4s.field.{Field, FieldType}
import com.outr.lucene4s.keyword.KeywordIndexing
import com.outr.lucene4s.mapper.{BaseSearchable, SearchableMacro}
import com.outr.lucene4s.query.{GroupedSearchTerm, QueryBuilder, SearchResult, SearchTerm}
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.facet.FacetsConfig
import org.apache.lucene.facet.taxonomy.directory.{DirectoryTaxonomyReader, DirectoryTaxonomyWriter}
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.{FSDirectory, RAMDirectory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.experimental.macros

class Lucene(val directory: Option[Path] = None,
             val appendIfExists: Boolean = true,
             val defaultFullTextSearchable: Boolean = false) {
  private[lucene4s] lazy val standardAnalyzer = new StandardAnalyzer

  private[lucene4s] lazy val system = ActorSystem()

  private lazy val indexPath = directory.map(_.resolve("index"))
  private lazy val taxonomyPath = directory.map(_.resolve("taxonomy"))

  private lazy val indexDirectory = indexPath.map(FSDirectory.open).getOrElse(new RAMDirectory)
  private lazy val taxonomyDirectory = taxonomyPath.map(FSDirectory.open).getOrElse(new RAMDirectory)

  private lazy val indexWriterConfig = new IndexWriterConfig(standardAnalyzer)
    .setOpenMode(if (appendIfExists) OpenMode.CREATE_OR_APPEND else OpenMode.CREATE)
  private[lucene4s] lazy val facetsConfig = new FacetsConfig

  private[lucene4s] lazy val indexWriter = new IndexWriter(indexDirectory, indexWriterConfig)
  private[lucene4s] lazy val taxonomyWriter = new DirectoryTaxonomyWriter(taxonomyDirectory)

  private[lucene4s] lazy val taxonomyReader = {
    taxonomyWriter.commit()
    new DirectoryTaxonomyReader(taxonomyDirectory)
  }

  private var currentIndexReader: Option[DirectoryReader] = None

  private var listeners: List[LuceneListener] = Nil

  val create = new LuceneCreate(this)

  lazy val fullText: Field[String] = create.field[String]("fullText")

  def doc(): DocumentBuilder = new DocumentBuilder(this, None)
  def update(searchTerm: SearchTerm): DocumentBuilder = new DocumentBuilder(this, Some(searchTerm))
  def delete(term: SearchTerm): Unit = {
    indexWriter.deleteDocuments(term.toLucene(this))
  }

  def query(): QueryBuilder[SearchResult] = QueryBuilder(this, conversion = sr => sr)

  def listen(listener: LuceneListener): Unit = synchronized {
    listeners = listeners ::: List(listener)
  }

  def commit(): Unit = {
    indexWriter.commit()
    taxonomyWriter.commit()
    listeners.foreach(_.commit())
  }

  /**
    * Deletes all documents in the index.
    */
  def deleteAll(): Unit = {
    indexWriter.deleteAll()
    listeners.foreach(_.delete())
  }

  def dispose(): Unit = {
    currentIndexReader.foreach(_.close())
    indexWriter.close()
    taxonomyWriter.close()
    indexDirectory.close()
    taxonomyDirectory.close()
  }

  private[lucene4s] def indexReader: DirectoryReader = synchronized {
    val reader = currentIndexReader match {
      case Some(r) => Option(DirectoryReader.openIfChanged(r, indexWriter, true)) match {
        case Some(updated) if updated ne r => {         // New reader was assigned
          system.scheduler.scheduleOnce(30.seconds) {
            r.close()
          }
          updated
        }
        case _ => r                                     // null was returned
      }
      case None => DirectoryReader.open(indexWriter, true, true)
    }
    currentIndexReader = Some(reader)
    reader
  }

  private[lucene4s] def indexed(builder: DocumentBuilder): Unit = synchronized {
    listeners.foreach(_.indexed(builder))
  }

  private[lucene4s] def searcher: IndexSearcher = new IndexSearcher(indexReader)
}

class LuceneCreate(val lucene: Lucene) {
  def field[T](name: String,
               fieldType: FieldType = FieldType.Stored,
               fullTextSearchable: Boolean = lucene.defaultFullTextSearchable
              )(implicit support: ValueSupport[T]): Field[T] = {
    new Field[T](name, fieldType, support, fullTextSearchable)
  }
  def facet(name: String,
            hierarchical: Boolean = false,
            multiValued: Boolean = false,
            requireDimCount: Boolean = false): FacetField = {
    lucene.facetsConfig.setHierarchical(name, hierarchical)
    lucene.facetsConfig.setMultiValued(name, multiValued)
    lucene.facetsConfig.setRequireDimCount(name, requireDimCount)
    FacetField(name, hierarchical, multiValued, requireDimCount)
  }
  def searchable[S <: BaseSearchable]: S = macro SearchableMacro.generate[S]
}