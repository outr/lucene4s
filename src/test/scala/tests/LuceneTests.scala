package tests

import java.io.File

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.{Document, Field, TextField}
import org.apache.lucene.facet.{FacetsCollector, FacetsConfig}
import org.apache.lucene.facet.sortedset.{DefaultSortedSetDocValuesReaderState, SortedSetDocValuesFacetCounts, SortedSetDocValuesFacetField}
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.RAMDirectory
import org.scalatest.{Matchers, WordSpec}

class LuceneTests extends WordSpec with Matchers {
  val analyzer = new StandardAnalyzer
  val directory = new RAMDirectory
  val config = new IndexWriterConfig(analyzer)
  val writer = new IndexWriter(directory, config)
  var reader = DirectoryReader.open(writer)

  val tagConfig = new FacetsConfig {
    setIndexFieldName("tag", "facet_tag")
  }

  "Lucene" should {
    "create a simple index" in {
      val doc = new Document
      doc.add(new Field("name", "John Doe", TextField.TYPE_STORED))
      doc.add(new SortedSetDocValuesFacetField("tag", "fake"))
      writer.addDocument(tagConfig.build(doc))
    }
    "query for the index" in {
      reader = DirectoryReader.openIfChanged(reader)

      val searcher = new IndexSearcher(reader)
      val parser = new QueryParser("name", analyzer)
      val query = parser.parse("john")
      val hits = searcher.search(query, 10).scoreDocs
      hits.length should be(1)
      hits.foreach { hit =>
        val doc = searcher.doc(hit.doc)
        doc.get("name") should be("John Doe")
      }
    }
    "query tags" in {
      reader = DirectoryReader.openIfChanged(reader)

      val state = new DefaultSortedSetDocValuesReaderState(reader, "facet_tag")
      val collector = new FacetsCollector
      val searcher = new IndexSearcher(reader)
      val parser = new QueryParser("name", analyzer)
      val query = parser.parse("john")
      FacetsCollector.search(searcher, query, 10, collector)
      val facets = new SortedSetDocValuesFacetCounts(state, collector)
      val result = facets.getTopChildren(10, "tag")
      (0 until result.childCount).foreach { index =>
        val lv = result.labelValues(index)
        println(s"Label: ${lv.label}, Value: ${lv.value}")
      }
    }
    "close" in {
      writer.close()
      reader.close()
      directory.close()
    }
  }
}
