package com.outr.lucene4s

import java.nio.file.Path

import com.outr.lucene4s.document.DocumentBuilder
import com.outr.lucene4s.field.Field
import com.outr.lucene4s.query.SearchTerm
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.facet.FacetsConfig
import org.apache.lucene.facet.taxonomy.SearcherTaxonomyManager
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter
import org.apache.lucene.index.{IndexReader, IndexWriter}

class ReplaceableLucene extends Lucene {
  private var _instance: Lucene = _

  def this(instance: Lucene) = {
    this()
    _instance = instance
  }

  override def uniqueFields: List[String] = instance.uniqueFields

  protected def instance: Lucene = _instance
  protected def instance_=(instance: Lucene): Unit = synchronized {
    _instance = instance
  }

  override def directory: Option[Path] = instance.directory

  override def defaultFullTextSearchable: Boolean = instance.defaultFullTextSearchable

  override def fullText: Field[String] = instance.fullText

  override def stopWords: Set[String] = instance.stopWords

  override def delete(term: SearchTerm): Unit = instance.delete(term)

  override def deleteAll(): Unit = instance.deleteAll()

  override def listen(listener: LuceneListener): Unit = instance.listen(listener)

  override def commit(): Unit = instance.commit()

  override def dispose(): Unit = instance.dispose()

  override protected[lucene4s] def analyzer: Analyzer = instance.analyzer

  override protected[lucene4s] def facetsConfig: FacetsConfig = instance.facetsConfig

  override protected[lucene4s] def taxonomyWriter: DirectoryTaxonomyWriter = instance.taxonomyWriter

  override protected[lucene4s] def indexWriter: IndexWriter = instance.indexWriter

  override protected[lucene4s] def indexReader: IndexReader = instance.indexReader

  override protected[lucene4s] def indexed(builders: Seq[DocumentBuilder]): Unit = instance.indexed(builders)

  override protected[lucene4s] def withSearcherAndTaxonomy[R](f: SearcherTaxonomyManager.SearcherAndTaxonomy => R): R = {
    instance.withSearcherAndTaxonomy(f)
  }
}