package com.outr.lucene4s

import java.nio.file.{Path, Paths}

import org.apache.lucene.analysis.core.WhitespaceAnalyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.{Document, Field}
import org.apache.lucene.facet.FacetsConfig
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{IndexSearcher, ScoreDoc, Sort, TopDocs}
import org.apache.lucene.store.{FSDirectory, RAMDirectory}
import org.apache.lucene.util.BytesRef

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

  def dispose(): Unit = {
    indexWriter.close()
    taxonomyWriter.close()
    indexDirectory.close()
    taxonomyDirectory.close()
  }

  private def indexReader: DirectoryReader = synchronized {
    val reader = currentIndexReader match {
      case Some(r) => DirectoryReader.openIfChanged(r)
      case None => DirectoryReader.open(indexWriter)
    }
    currentIndexReader = Some(reader)
    reader
  }

  private[lucene4s] def store(document: Document): Unit = {
    indexWriter.addDocument(facetsConfig.build(taxonomyWriter, document))
  }
}

case class QueryBuilder private[lucene4s](lucene: Lucene,
                                          defaultField: String,
                                          limit: Int = 10,
                                          scoreDocs: Boolean = false,
                                          scoreMax: Boolean = false) {
  def limit(v: Int): QueryBuilder = copy(limit = v)

  def scoreDocs(b: Boolean = true): QueryBuilder = copy(scoreDocs = b)

  def scoreMax(b: Boolean = true): QueryBuilder = copy(scoreMax = b)

  def search(query: String): SearchResults = {
    val parser = new QueryParser(defaultField, lucene.standardAnalyzer)
    val q = parser.parse(query)
    val sort = Sort.INDEXORDER
    val topDocs = lucene.searcher.search(q, limit, sort, scoreDocs, scoreMax)
    new SearchResults(lucene, topDocs)
  }
}

class SearchResults private[lucene4s](lucene: Lucene, topDocs: TopDocs) {
  lazy val results: Vector[SearchResult] = topDocs.scoreDocs.toVector.map(sd => new SearchResult(lucene, this, sd))
}

class SearchResult private[lucene4s](lucene: Lucene, search: SearchResults, scoreDoc: ScoreDoc) {
  private lazy val doc = lucene.searcher.doc(scoreDoc.doc)

  def string(name: String): String = doc.getField(name).stringValue()
  def bytesRef(name: String): BytesRef = doc.getField(name).binaryValue()
  def numeric(name: String): Number = doc.getField(name).numericValue()
  def int(name: String): Int = numeric(name).intValue()
  def double(name: String): Double = numeric(name).doubleValue()
}

class DocumentBuilder(lucene: Lucene, document: Document = new Document) {
  def field(name: String, value: FieldValue, fieldType: FieldType = FieldType.Stored): DocumentBuilder = {
    val ft = fieldType.toLucene()
    val f = value match {
      case StringFieldValue(v) => new Field(name, v, ft)
      case ByteArrayFieldValue(v) => new Field(name, v, ft)
      case BytesRefFieldValue(v) => new Field(name, v, ft)
    }
    println(s"Field: $f")
    document.add(f)
    this
  }

  def index(): Unit = lucene.store(document)
}

sealed trait FieldValue

case class StringFieldValue(value: String) extends FieldValue

case class ByteArrayFieldValue(value: Array[Byte]) extends FieldValue

case class BytesRefFieldValue(value: BytesRef) extends FieldValue

case class FieldType(stored: Boolean = false,
                     tokenized: Boolean = true,
                     storeTermVectors: Boolean = false,
                     storeTermVectorOffsets: Boolean = false,
                     storeTermVectorPositions: Boolean = false,
                     storeTermVectorPayloads: Boolean = false,
                     omitNorms: Boolean = false,
                     indexOptions: Set[IndexOption] = Set.empty,
                     frozen: Boolean = false,
                     docValuesType: DocValuesType = DocValuesType.None,
                     dimensionCount: Int = 0,
                     dimensionNumBytes: Int = 0) {
  private[lucene4s] def toLucene(): org.apache.lucene.document.FieldType = {
    val ft = new org.apache.lucene.document.FieldType
    ft.setStored(stored)
    ft.setTokenized(tokenized)
    ft.setStoreTermVectors(storeTermVectors)
    ft.setStoreTermVectorOffsets(storeTermVectorOffsets)
    ft.setStoreTermVectorPositions(storeTermVectorPositions)
    ft.setStoreTermVectorPayloads(storeTermVectorPayloads)
    ft.setOmitNorms(omitNorms)

    import org.apache.lucene.index.{IndexOptions => IO}
    val io = if (indexOptions.contains(IndexOption.Offsets)) {
      IO.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS
    } else if (indexOptions.contains(IndexOption.Positions)) {
      IO.DOCS_AND_FREQS_AND_POSITIONS
    } else if (indexOptions.contains(IndexOption.Frequencies)) {
      IO.DOCS_AND_FREQS
    } else if (indexOptions.contains(IndexOption.Documents)) {
      IO.DOCS
    } else {
      IO.NONE
    }
    ft.setIndexOptions(io)
    ft.setDocValuesType(docValuesType.value)
    ft.setDimensions(dimensionCount, dimensionNumBytes)
    if (frozen) ft.freeze()
    ft
  }
}

object FieldType {
  val NotStored = FieldType(
    indexOptions = Set(IndexOption.Documents, IndexOption.Frequencies, IndexOption.Positions),
    tokenized = true,
    frozen = true
  )
  val Stored = FieldType(
    indexOptions = Set(IndexOption.Documents, IndexOption.Frequencies, IndexOption.Positions),
    tokenized = true,
    stored = true,
    frozen = true
  )
}

sealed trait IndexOption

object IndexOption {
  case object Documents extends IndexOption
  case object Frequencies extends IndexOption
  case object Positions extends IndexOption
  case object Offsets extends IndexOption
}

sealed abstract class DocValuesType(private[lucene4s] val value: org.apache.lucene.index.DocValuesType)

object DocValuesType {
  import org.apache.lucene.index.{DocValuesType => DVT}

  case object None extends DocValuesType(DVT.NONE)
  case object Numeric extends DocValuesType(DVT.NUMERIC)
  case object Binary extends DocValuesType(DVT.BINARY)
  case object Sorted extends DocValuesType(DVT.SORTED)
  case object SortedNumeric extends DocValuesType(DVT.SORTED_NUMERIC)
  case object SortedSet extends DocValuesType(DVT.SORTED_SET)
}