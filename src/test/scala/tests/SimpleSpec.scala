package tests

import com.outr.lucene4s._
import com.outr.lucene4s.query.{Condition, Sort}
import org.scalatest.{Matchers, WordSpec}

class SimpleSpec extends WordSpec with Matchers {
  val lucene = new Lucene()
  val name = lucene.create.field[String]("name")
  val age = lucene.create.field[Int]("age")
  val progress = lucene.create.field[Double]("progress")
  val bytes = lucene.create.field[Long]("bytes")

  "Simple Spec" should {
    "create a simple document" in {
      lucene.doc().fields(name("John Doe"), age(23), progress(0.123), bytes(123456789L)).index()
    }
    "query for the index" in {
      val paged = lucene.query().search()
      paged.total should be(1)
      val results = paged.results
      results.length should be(1)
      results(0)(name) should be("John Doe")
      results(0)(age) should be(23)
      results(0)(progress) should be(0.123)
      results(0)(bytes) should be(123456789L)
    }
    "add a few more documents" in {
      lucene.doc().fields(name("Jane Doe")).index()
      lucene.doc().fields(name("Andrew Anderson")).index()
      lucene.doc().fields(name("Billy Bob")).index()
      lucene.doc().fields(name("Carly Charles")).index()
    }
    "query using pagination" in {
      val page1 = lucene.query().limit(2).search()
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
      val paged = lucene.query().sort(Sort(name)).search()
      paged.total should be(5)
      paged.results(0)(name) should be("Andrew Anderson")
      paged.results(1)(name) should be("Billy Bob")
      paged.results(2)(name) should be("Carly Charles")
      paged.results(3)(name) should be("Jane Doe")
      paged.results(4)(name) should be("John Doe")
    }
    "query sorting by name reversed" in {
      val paged = lucene.query().sort(Sort(name, reverse = true)).search()
      paged.total should be(5)
      paged.results(0)(name) should be("John Doe")
      paged.results(1)(name) should be("Jane Doe")
      paged.results(2)(name) should be("Carly Charles")
      paged.results(3)(name) should be("Billy Bob")
      paged.results(4)(name) should be("Andrew Anderson")
    }
    "query by last name" in {
      val paged = lucene.query().scoreDocs().sort(Sort.Score).filter(term(name("doe"))).search()
      paged.total should be(2)
      paged.results(0)(name) should be("John Doe")
      paged.results(0).score should be(0.7854939103126526)
      paged.results(1)(name) should be("Jane Doe")
      paged.results(1).score should be(0.7854939103126526)
    }
    "query by age" in {
      val paged = lucene.query().filter(exact(age(23))).search()
      paged.total should be(1)
      paged.results(0)(name) should be("John Doe")
    }
    "query by progress" in {
      val paged = lucene.query().filter(exact(progress(0.123))).search()
      paged.total should be(1)
      paged.results(0)(name) should be("John Doe")
    }
    "query by bytes" in {
      val paged = lucene.query().filter(exact(bytes(123456789L))).search()
      paged.total should be(1)
      paged.results(0)(name) should be("John Doe")
    }
    "query fuzzy matching john and jane" in {
      val paged = lucene.query().scoreDocs().sort(Sort.Score).filter(fuzzy(name("jhn"))).search()
      paged.total should be(2)
      paged.results(0)(name) should be("John Doe")
      paged.results(0).score should be(0.829213559627533)
      paged.results(1)(name) should be("Jane Doe")
      paged.results(1).score should be(0.4146067798137665)
    }
    "update 'Billy Bob' to 'Johnny Bob'" in {
      lucene.update(term(name("Billy Bob"))).fields(name("Johnny Bob")).index()
    }
    "query by part of first name with wildcard" in {
      val paged = lucene.query().scoreDocs().sort(Sort.Score).filter(wildcard(name("john*"))).search()
      paged.total should be(2)
      paged.results(0)(name) should be("John Doe")
      paged.results(0).score should be(1.0)
      paged.results(1)(name) should be("Johnny Bob")
      paged.results(1).score should be(1.0)
    }
    "query with two terms" in {
      val paged = lucene.query().filter(grouped(
        wildcard(name("john*")) -> Condition.Must,
        term(name("doe")) -> Condition.MustNot
      )).search()
      paged.total should be(1)
      paged.results(0)(name) should be("Johnny Bob")
    }
    "delete John Doe" in {
      lucene.query().search().results.length should be(6)
      lucene.delete(term(name("john")))
      lucene.query().search().results.length should be(5)
    }
    // TODO: storage and querying of Int, Long, Double, Boolean, Array[Byte]
    // TODO: storage and querying of multiple points
    // TODO: storage and querying of lat/long
    // TODO: querying ranges
    // TODO: storage and querying of dates
    "dispose" in {
      lucene.dispose()
    }
  }
}
