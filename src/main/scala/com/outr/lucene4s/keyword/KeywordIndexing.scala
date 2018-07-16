package com.outr.lucene4s.keyword

import java.util.{Timer, TimerTask}

import com.outr.lucene4s._
import com.outr.lucene4s.document.DocumentBuilder
import com.outr.lucene4s.facet.FacetField
import com.outr.lucene4s.field.FieldType
import com.outr.lucene4s.field.value.FieldAndValue
import org.apache.lucene.document.{Document, Field}
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{IndexSearcher, MatchAllDocsQuery}
import org.apache.lucene.store.{NIOFSDirectory, RAMDirectory}

import scala.annotation.tailrec
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
  * @param allowedCharacters a String containing all the characters allowed. This defaults to a-z, A-Z, 0-9,
  *                          period and space. Keywords are filtered to remove any characters not allowed.
  * @param removeEndsWithCharacters removes any of these characters found at the end of the keyword. Primarily
  *                                 useful for removing punctuation. Defaults to ",.?!;".
  * @param minimumLength the minimum length of keywords to be included. defaults to 2.
  */
case class KeywordIndexing(lucene: Lucene,
                           directoryName: String,
                           wordsFromBuilder: DocumentBuilder => List[String] = KeywordIndexing.DefaultWordsFromBuilder,
                           includeFields: List[com.outr.lucene4s.field.Field[_]] = Nil,
                           stopWords: Set[String] = KeywordIndexing.DefaultStopWords,
                           allowedCharacters: String = KeywordIndexing.DefaultAllowedCharacters,
                           removeEndsWithCharacters: String = KeywordIndexing.DefaultRemoveEndsWithCharacters,
                           minimumLength: Int = 2) extends LuceneListener {
  // Write support
  private lazy val indexPath = lucene.directory.map(_.resolve(directoryName))
  private lazy val indexDirectory = indexPath.map(new NIOFSDirectory(_)).getOrElse(new RAMDirectory)
  private lazy val indexWriterConfig = new IndexWriterConfig(lucene.analyzer)
  private lazy val indexWriter = new IndexWriter(indexDirectory, indexWriterConfig)

  private lazy val allowedCharactersSet = allowedCharacters.toCharArray.toSet
  private lazy val removeEndsWithCharactersSet = removeEndsWithCharacters.toCharArray.toSet

  // Read support
  private var currentIndexReader: Option[DirectoryReader] = None

  // Automatically add the listener
  lucene.listen(this)

  private def indexReader: DirectoryReader = synchronized {
    val reader = currentIndexReader match {
      case Some(r) => Option(DirectoryReader.openIfChanged(r, indexWriter, true)) match {
        case Some(updated) if updated ne r => {         // New reader was assigned
          val timer = new Timer
          timer.schedule(new TimerTask {
            override def run(): Unit = r.close()
          }, 30.seconds.toMillis)
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
    indexableWords.foreach { unfilteredWord =>
      val word = removeEndsWith(unfilteredWord.filter(allowedCharactersSet.contains).trim)
      if (word.length >= minimumLength) {
        val doc = new Document
        doc.add(new Field("keyword", word, FieldType.Stored.lucene()))
        additionalValues.foreach { fv =>
          doc.add(new Field(fv.field.name, fv.value.toString, FieldType.Stored.lucene()))
        }
        val parser = new QueryParser("keyword", lucene.analyzer)
        val queryString = new StringBuilder(s""""$word"""")
        additionalValues.foreach { fv =>
          queryString.append(s" AND ${fv.field.name}:${fv.value.toString}")
        }
        val query = parser.parse(queryString.toString)
        indexWriter.deleteDocuments(query)
        indexWriter.addDocument(doc)
      }
    }
  }

  @tailrec
  private def removeEndsWith(word: String): String = {
    if (word.isEmpty || !removeEndsWithCharactersSet.contains(word.charAt(word.length - 1))) {
      word
    } else {
      removeEndsWith(word.substring(0, word.length - 1))
    }
  }

  def search(queryString: String = "", limit: Int = 10): KeywordResults = {
    val searcher = this.searcher
    val query = queryString match {
      case "" => new MatchAllDocsQuery
      case _ => {
        val parser = new QueryParser("keyword", lucene.analyzer)
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
      val wordMatch = WordMatch(queryString, word)
      KeywordResult(word, wordMatch, scoreDoc.score.toDouble, additionalFields)
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
  val DefaultWordsFromBuilder: DocumentBuilder => List[String] = (builder: DocumentBuilder) => builder.fullText.flatMap(_.split(DefaultSplitRegex))
  val DefaultAllowedCharacters: String = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789. "
  val DefaultRemoveEndsWithCharacters: String = ",.?!;"

  def FieldFromBuilder[T](field: com.outr.lucene4s.field.Field[T]): DocumentBuilder => List[String] = (builder: DocumentBuilder) => {
    val text = Option(builder.document.get(field.name)).map(_.trim).getOrElse("")
    if (text.isEmpty) {
      Nil
    } else {
      List(text)
    }
  }
  def FieldWordsFromBuilder[T](field: com.outr.lucene4s.field.Field[T]): DocumentBuilder => List[String] = (builder: DocumentBuilder) => {
    builder.document.get(field.name).split(DefaultSplitRegex).toList
  }
  def FacetFromBuilder(field: FacetField, pathSeparator: Option[String] = None): DocumentBuilder => List[String] = (builder: DocumentBuilder) => {
    builder.facetsForField(field).map { fv =>
      pathSeparator match {
        case Some(ps) => fv.path.mkString(ps)
        case None => fv.path.head
      }
    }
  }
}