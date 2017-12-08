package com.outr.lucene4s

import java.nio.file.Path

import akka.actor.ActorSystem
import com.outr.lucene4s.document.DocumentBuilder
import com.outr.lucene4s.facet.FacetField
import com.outr.lucene4s.field.value.support.ValueSupport
import com.outr.lucene4s.field.{Field, FieldType}
import com.outr.lucene4s.keyword.KeywordIndexing
import com.outr.lucene4s.mapper.{BaseSearchable, SearchableMacro}
import com.outr.lucene4s.query.{QueryBuilder, SearchResult, SearchTerm}
import org.apache.lucene.analysis.CharArraySet
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.facet.FacetsConfig
import org.apache.lucene.facet.taxonomy.SearcherTaxonomyManager
import org.apache.lucene.facet.taxonomy.SearcherTaxonomyManager.SearcherAndTaxonomy
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter
import org.apache.lucene.facet.taxonomy.writercache.TaxonomyWriterCache
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.search.SearcherFactory
import org.apache.lucene.store.{FSDirectory, RAMDirectory}

import scala.language.experimental.macros
import scala.collection.JavaConverters._

class Lucene(val directory: Option[Path] = None,
             val appendIfExists: Boolean = true,
             val defaultFullTextSearchable: Boolean = false,
             val autoCommit: Boolean = false,
             stopWords: Set[String] = KeywordIndexing.DefaultStopWords,
             stopWordsIgnoreCase: Boolean = true) {
  private[lucene4s] lazy val standardAnalyzer = new StandardAnalyzer(new CharArraySet(stopWords.asJava, stopWordsIgnoreCase))

  private[lucene4s] lazy val system = ActorSystem()

  private lazy val indexPath = directory.map(_.resolve("index"))
  private lazy val taxonomyPath = directory.map(_.resolve("taxonomy"))

  private lazy val indexDirectory = indexPath.map(FSDirectory.open).getOrElse(new RAMDirectory)
  private lazy val taxonomyDirectory = taxonomyPath.map(FSDirectory.open).getOrElse(new RAMDirectory)

  private lazy val indexWriterConfig = new IndexWriterConfig(standardAnalyzer)
    .setOpenMode(if (appendIfExists) OpenMode.CREATE_OR_APPEND else OpenMode.CREATE)
  private[lucene4s] lazy val facetsConfig = new FacetsConfig

  private[lucene4s] lazy val indexWriter = new IndexWriter(indexDirectory, indexWriterConfig)
  private[lucene4s] lazy val taxonomyWriterCache = createTaxonomyWriterCache()
  private[lucene4s] lazy val taxonomyWriter = new DirectoryTaxonomyWriter(taxonomyDirectory, IndexWriterConfig.OpenMode.CREATE_OR_APPEND, taxonomyWriterCache)
  private[lucene4s] lazy val searcherTaxonomyManager = new SearcherTaxonomyManager(
    indexWriter,
    new SearcherFactory,
    taxonomyWriter
  )

  protected def createTaxonomyWriterCache(): TaxonomyWriterCache = DirectoryTaxonomyWriter.defaultTaxonomyWriterCache()

  private[lucene4s] def withSearcherAndTaxonomy[R](f: SearcherAndTaxonomy => R): R = {
    searcherTaxonomyManager.maybeRefreshBlocking()
    val instance = searcherTaxonomyManager.acquire()
    try {
      f(instance)
    } finally {
      searcherTaxonomyManager.release(instance)
    }
  }

  private var listeners: List[LuceneListener] = Nil

  val create = new LuceneCreate(this)

  lazy val fullText: Field[String] = create.field[String]("fullText")

  def doc(): DocumentBuilder = new DocumentBuilder(this, None)
  def update(searchTerm: SearchTerm): DocumentBuilder = new DocumentBuilder(this, Some(searchTerm))
  def delete(term: SearchTerm): Unit = {
    indexWriter.deleteDocuments(term.toLucene(this))
    if (autoCommit) {
      commit()
    }
  }

  def query(): QueryBuilder[SearchResult] = QueryBuilder(this, conversion = sr => sr)

  def listen(listener: LuceneListener): Unit = synchronized {
    listeners = listeners ::: List(listener)
  }

  def commit(): Unit = {
    indexWriter.commit()
    taxonomyWriter.commit()
    searcherTaxonomyManager.maybeRefresh()
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
    withSearcherAndTaxonomy { instance =>
      instance.searcher.getIndexReader.close()
    }
    indexWriter.close()
    taxonomyWriter.close()
    indexDirectory.close()
    taxonomyDirectory.close()
  }

  private[lucene4s] def indexed(builder: DocumentBuilder): Unit = synchronized {
    listeners.foreach(_.indexed(builder))
    if (autoCommit) {
      commit()
    }
  }
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

object Lucene {
  private val specialCharacters = Set('~', '*', '?', '^', ':', '(', ')')

  def isLuceneWord(word: String): Boolean = specialCharacters.exists(c => word.contains(c))
  def removeSpecialCharacters(text: String): String = text.filterNot(specialCharacters.contains)
  def queryToWords(query: String): List[String] = query.split(' ').toList.collect {
    case w if !w.equalsIgnoreCase("AND") && !w.equalsIgnoreCase("OR") => {
      val colon = w.indexOf(':')
      val term = if (colon > -1) {
        w.substring(colon + 1)
      } else {
        w
      }
      removeSpecialCharacters(term)
    }
  }
}