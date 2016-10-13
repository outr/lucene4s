package tests

import com.outr.lucene4s._
import com.outr.lucene4s.query.Sort
import org.scalatest.{Matchers, WordSpec}

class SimpleSpec extends WordSpec with Matchers {
  val lucene = new Lucene()
  val name = lucene.create.field[String]("name")

  "Simple Spec" should {
    "create a simple document" in {
      lucene.doc().fields(name("John Doe")).index()
    }
    "query for the index" in {
      val paged = lucene.query(name).search("john")
      paged.total should be(1)
      val results = paged.results
      results.length should be(1)
      results(0)(name) should be("John Doe")
    }
    "add a few more documents" in {
      lucene.doc().fields(name("Jane Doe")).index()
      lucene.doc().fields(name("Andrew Anderson")).index()
      lucene.doc().fields(name("Billy Bob")).index()
      lucene.doc().fields(name("Carly Charles")).index()
    }
    "query using pagination" in {
      val page1 = lucene.query(name).limit(2).search("*:*")
      page1.total should be(5)
      val results1 = page1.results
      page1.pageIndex should be(0)
      page1.pages should be(3)
      results1.length should be(2)
      results1(0)(name) should be("John Doe")
      results1(1)(name) should be("Jane Doe")
      page1.hasPreviousPage should be(false)

      val page2 = page1.nextPage().get
      page2.total should be(5)
      val results2 = page2.results
      page2.pageIndex should be(1)
      page2.pages should be(3)
      results2.length should be(2)
      results2(0)(name) should be("Andrew Anderson")
      results2(1)(name) should be("Billy Bob")

      val page3 = page2.nextPage().get
      page3.total should be(5)
      val results3 = page3.results
      page3.pageIndex should be(2)
      page3.pages should be(3)
      results3.length should be(1)
      results3(0)(name) should be("Carly Charles")
      page3.hasNextPage should be(false)
    }
    "query sorting by name" in {
      val paged = lucene.query(name).sort(Sort(name)).search()
      paged.total should be(5)
      paged.results(0)(name) should be("Andrew Anderson")
      paged.results(1)(name) should be("Billy Bob")
      paged.results(2)(name) should be("Carly Charles")
      paged.results(3)(name) should be("Jane Doe")
      paged.results(4)(name) should be("John Doe")
    }
    "query sorting by name reversed" in {
      val paged = lucene.query(name).sort(Sort(name, reverse = true)).search()
      paged.total should be(5)
      paged.results(0)(name) should be("John Doe")
      paged.results(1)(name) should be("Jane Doe")
      paged.results(2)(name) should be("Carly Charles")
      paged.results(3)(name) should be("Billy Bob")
      paged.results(4)(name) should be("Andrew Anderson")
    }
    "query by last name" in {
      val paged = lucene.query(name).scoreDocs().sort(Sort.Score).search("doe")
      paged.total should be(2)
      paged.results(0)(name) should be("John Doe")
      paged.results(0).score should be(0.7854939103126526)
      paged.results(1)(name) should be("Jane Doe")
      paged.results(1).score should be(0.7854939103126526)
    }
    "query by part of first name" in {
      val paged = lucene.query(name).leadingWildcardSupport().scoreDocs().sort(Sort.Score).search("*ohn")
      paged.total should be(1)
      paged.results(0)(name) should be("John Doe")
      paged.results(0).score should be(1.0)
    }
    // TODO: storage and querying of Int, Long, Double, Boolean, Array[Byte]
    // TODO: storage and querying of multiple points
    // TODO: storage and querying of lat/long
    "dispose" in {
      lucene.dispose()
    }
  }
}
