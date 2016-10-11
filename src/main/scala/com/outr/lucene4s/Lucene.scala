package com.outr.lucene4s

import java.nio.file.Path

import com.outr.lucene4s.document.DocumentBuilder
import com.outr.lucene4s.query.QueryBuilder
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.facet.FacetsConfig
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.{FSDirectory, RAMDirectory}

class Lucene(directory: Option[Path] = None, appendIfExists: Boolean = true) {
  private[lucene4s] lazy val standardAnalyzer = new StandardAnalyzer

  private lazy val indexPath = directory.map(_.resolve("index"))
  private lazy val taxonomyPath = directory.map(_.resolve("taxonomy"))

  private lazy val indexDirectory = indexPath.map(FSDirectory.open).getOrElse(new RAMDirectory)
  private lazy val taxonomyDirectory = taxonomyPath.map(FSDirectory.open).getOrElse(new RAMDirectory)

  private lazy val indexWriterConfig = new IndexWriterConfig(standardAnalyzer)
    .setOpenMode(if (appendIfExists) OpenMode.CREATE_OR_APPEND else OpenMode.CREATE)
  private lazy val facetsConfig = new FacetsConfig

  private lazy val indexWriter = new IndexWriter(indexDirectory, indexWriterConfig)
  private lazy val taxonomyWriter = new DirectoryTaxonomyWriter(taxonomyDirectory)

  private var currentIndexReader: Option[DirectoryReader] = None

  private[lucene4s] lazy val searcher = new IndexSearcher(indexReader)

  def doc(): DocumentBuilder = new DocumentBuilder(this)

  def query(defaultField: String): QueryBuilder = QueryBuilder(this, defaultField)

  def flush(): Unit = {
    indexWriter.flush()
    indexWriter.commit()
  }

  def dispose(): Unit = {
    indexWriter.close()
    taxonomyWriter.close()
    indexDirectory.close()
    taxonomyDirectory.close()
  }

  private def indexReader: DirectoryReader = synchronized {
    val reader = currentIndexReader match {
      case Some(r) => DirectoryReader.openIfChanged(r, indexWriter, true)
      case None => DirectoryReader.open(indexWriter, true, true)
    }
    currentIndexReader = Some(reader)
    reader
  }

  private[lucene4s] def store(document: Document): Unit = {
    indexWriter.addDocument(facetsConfig.build(taxonomyWriter, document))
  }
}