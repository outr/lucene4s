package com.outr.lucene4s.keyword

import com.outr.lucene4s._
import com.outr.lucene4s.document.DocumentBuilder
import com.outr.lucene4s.field.FieldType
import com.outr.lucene4s.field.value.FieldAndValue
import com.outr.scribe.Logging
import org.apache.lucene.document.{Document, Field}
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{IndexSearcher, MatchAllDocsQuery}
import org.apache.lucene.store.{FSDirectory, RAMDirectory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * KeywordIndexing allows the automatic generation of another index specifically for keyword searching without
  * duplicates.
  *
  * @param lucene the Lucene instance to apply to
  * @param directoryName the directory name to derive the storage location from the existing index
  * @param wordsFromBuilder the function that extracts the words from the DocumentBuilder for inclusion
  * @param includeFields a list of additional fields to include from the DocumentBuilder during indexing
  * @param stopWords a list of exclusion words not to be indexed
  * @param wordMatcherRegex a regular expression to limit the type of words included in the index
  */
case class KeywordIndexing(lucene: Lucene,
                           directoryName: String,
                           wordsFromBuilder: DocumentBuilder => List[String] = KeywordIndexing.DefaultWordsFromBuilder,
                           includeFields: List[com.outr.lucene4s.field.Field[_]] = Nil,
                           stopWords: Set[String] = KeywordIndexing.DefaultStopWords,
                           wordMatcherRegex: String = """[a-zA-Z0-9.]{2,}""") extends LuceneListener with Logging {
  // Write support
  private lazy val indexPath = lucene.directory.map(_.resolve(directoryName))
  private lazy val indexDirectory = indexPath.map(FSDirectory.open).getOrElse(new RAMDirectory)
  private lazy val indexWriterConfig = new IndexWriterConfig(lucene.standardAnalyzer)
  private lazy val indexWriter = new IndexWriter(indexDirectory, indexWriterConfig)

  // Read support
  private var currentIndexReader: Option[DirectoryReader] = None

  // Automatically add the listener
  lucene.listen(this)

  private def indexReader: DirectoryReader = synchronized {
    val reader = currentIndexReader match {
      case Some(r) => Option(DirectoryReader.openIfChanged(r, indexWriter, true)) match {
        case Some(updated) if updated ne r => {         // New reader was assigned
          lucene.system.scheduler.scheduleOnce(30.seconds) {
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
  private def searcher: IndexSearcher = new IndexSearcher(indexReader)

  override def indexed(builder: DocumentBuilder): Unit = {
    val additionalValues = includeFields.flatMap { field =>
      builder.valueForName(field.name)
    }
    index(wordsFromBuilder(builder), additionalValues)
  }

  override def commit(): Unit = indexWriter.commit()

  override def delete(): Unit = {
    indexWriter.deleteAll()
  }

  def index(words: List[String], additionalValues: List[FieldAndValue[_]]): Unit = if (words.nonEmpty) {
    val indexableWords = words.filterNot(stopWords.contains)
    indexableWords.foreach {
      case word if word.matches(wordMatcherRegex) => {
        val doc = new Document
        doc.add(new Field("keyword", word, FieldType.Stored.lucene()))
        additionalValues.foreach { fv =>
          doc.add(new Field(fv.field.name, fv.value.toString, FieldType.Stored.lucene()))
        }
        val parser = new QueryParser("keyword", lucene.standardAnalyzer)
        val queryString = new StringBuilder(s"+$word")
        additionalValues.foreach { fv =>
          queryString.append(s" +${fv.field.name}:${fv.value.toString}")
        }
        val query = parser.parse(queryString.toString)
        indexWriter.deleteDocuments(query)
        indexWriter.addDocument(doc)
      }
      case _ => // Ignore empty words
    }
  }

  def search(queryString: String = "", limit: Int = 10): KeywordResults = {
    val searcher = this.searcher
    val query = queryString match {
      case "" => new MatchAllDocsQuery
      case _ => {
        val parser = new QueryParser("keyword", lucene.standardAnalyzer)
        parser.setAllowLeadingWildcard(true)
        parser.parse(queryString)
      }
    }
    val searchResults = searcher.search(query, limit)
    val keywords = searchResults.scoreDocs.map { scoreDoc =>
      val doc = searcher.doc(scoreDoc.doc)
      val word = doc.get("keyword")
      val additionalFields = includeFields.flatMap { f =>
        Option(f.name -> doc.get(f.name))
      }.toMap
      KeywordResult(word, scoreDoc.score.toDouble, additionalFields)
    }.toList
    KeywordResults(keywords, searchResults.totalHits, searchResults.getMaxScore)
  }
}

object KeywordIndexing {
  val DefaultStopWords: Set[String] = Set(
    "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "i", "if", "in", "into", "is",
    "no", "not", "of", "on", "or", "s", "such", "t", "that", "the", "their", "then", "there", "these",
    "they", "this", "to", "was", "will", "with"
  )
  val DefaultSplitRegex: String = """\s+"""
  val DefaultWordsFromBuilder: (DocumentBuilder) => List[String] = (builder: DocumentBuilder) => builder.fullText.flatMap(_.split(DefaultSplitRegex))

  def FieldFromBuilder[T](field: com.outr.lucene4s.field.Field[T]): (DocumentBuilder) => List[String] = (builder: DocumentBuilder) => {
    List(builder.document.get(field.name))
  }
  def FieldWordsFromBuilder[T](field: com.outr.lucene4s.field.Field[T]): (DocumentBuilder) => List[String] = (builder: DocumentBuilder) => {
    builder.document.get(field.name).split(DefaultSplitRegex).toList
  }
}

case class KeywordResults(results: List[KeywordResult], total: Int, maxScore: Double) {
  lazy val words: List[String] = results.map(_.word)
}

case class KeywordResult(word: String, score: Double, additionalFields: Map[String, String])