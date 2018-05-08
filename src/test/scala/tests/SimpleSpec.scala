package tests

import com.outr.lucene4s._
import com.outr.lucene4s.field.Field
import com.outr.lucene4s.keyword.KeywordIndexing
import com.outr.lucene4s.query._
import org.scalatest.{Matchers, WordSpec}

class SimpleSpec extends WordSpec with Matchers {
  val lucene = new DirectLucene(uniqueFields = List("name"), defaultFullTextSearchable = true, autoCommit = true)
  val keywordIndexing = KeywordIndexing(lucene, "keywords")
  val name: Field[String] = lucene.create.field[String]("name")
  val age: Field[Int] = lucene.create.field[Int]("age")
  val progress: Field[Double] = lucene.create.field[Double]("progress")
  val bytes: Field[Long] = lucene.create.field[Long]("bytes")
  val enabled: Field[Boolean] = lucene.create.field[Boolean]("enabled")

  "Simple Spec" should {
    "create a simple document" in {
      lucene.doc().fields(name("John Doe"), age(23), progress(0.123), bytes(123456789L), enabled(true)).index()
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
      results(0)(enabled) should be(true)
    }
    "add a few more documents" in {
      lucene.doc().fields(name("Jane Doe"), age(21)).index()
      lucene.doc().fields(name("Andrew Anderson"), age(31)).index()
      lucene.doc().fields(name("Billy Bob"), age(25)).index()
      lucene.doc().fields(name("Carly Charles"), age(19)).index()
    }
    "query by an exact name" in {
      val page = lucene.query().filter(exact(name("John Doe"))).search()
      page.total should be(1)
      val result = page.results.head
      result(name) should be("John Doe")
      result(age) should be(23)
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
    "query sorting by age" in {
      val paged = lucene.query().sort(Sort(age, reverse = true)).search()
      paged.total should be(5)
      paged.results(0)(name) should be("Andrew Anderson")
    }
    "query by age range" in {
      val paged = lucene.query().filter(intRange(age, 21, 25)).sort(Sort(age)).search()
      paged.total should be(3)
      paged.results(0)(name) should be("Jane Doe")
      paged.results(1)(name) should be("John Doe")
      paged.results(2)(name) should be("Billy Bob")
    }
    "query by age range with DSL" in {
      val paged = lucene.query().filter(age <=> (25, 21)).sort(Sort(age)).search()
      paged.total should be(3)
      paged.results(0)(name) should be("Jane Doe")
      paged.results(1)(name) should be("John Doe")
      paged.results(2)(name) should be("Billy Bob")
    }
    "query by age greater than or equal to 25" in {
      val paged = lucene.query().filter(age >= 25).sort(Sort(age)).search()
      paged.total should be(2)
      paged.results(0)(name) should be("Billy Bob")
      paged.results(1)(name) should be("Andrew Anderson")
    }
    "query by age using set" in {
      val paged = lucene.query().filter(age.contains(31, 19)).sort(Sort(age)).search()
      paged.total should be(2)
      paged.results(0)(name) should be("Carly Charles")
      paged.results(1)(name) should be("Andrew Anderson")
    }
    "query sorting by progress" in {
      val paged = lucene.query().sort(Sort(progress, reverse = true)).search()
      paged.total should be(5)
      paged.results(0)(name) should be("John Doe")
    }
    "query sorting by bytes" in {
      val paged = lucene.query().sort(Sort(bytes, reverse = true)).search()
      paged.total should be(5)
      paged.results(0)(name) should be("John Doe")
    }
    "query sorting by enabled" in {
      val paged = lucene.query().sort(Sort(enabled, reverse = true)).search()
      paged.total should be(5)
      paged.results(0)(name) should be("John Doe")
    }
    "query by last name" in {
      val paged = lucene.query().scoreDocs().sort(Sort.Score).filter(term(name("doe"))).search()
      paged.total should be(2)
      paged.results(0)(name) should be("John Doe")
      paged.results(0).score should be(0.8754687309265137)
      paged.results(1)(name) should be("Jane Doe")
      paged.results(1).score should be(0.8754687309265137)
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
    "query by enabled" in {
      val paged = lucene.query().filter(exact(enabled(true))).search()
      paged.total should be(1)
      paged.results(0)(enabled) should be(true)
      lucene.query().filter(exact(enabled(false))).search().total should be(0)
    }
    "query fuzzy matching john and jane" in {
      val paged = lucene.query().scoreDocs().sort(Sort.Score).filter(fuzzy(name("jhn"))).search()
      paged.total should be(2)
      paged.results(0)(name) should be("John Doe")
      paged.results(0).score should be(0.9241962432861328)
      paged.results(1)(name) should be("Jane Doe")
      paged.results(1).score should be(0.4620981216430664)
    }
    "query fuzzy matching john and jane with two word phrase" in {
      val paged = lucene.query().scoreDocs().sort(Sort.Score).filter(parseFuzzy("jhn doe", Some(name))).search()
      val names = paged.results.map(_.apply(name)).toSet
      names should be(Set("John Doe", "Jane Doe"))
      paged.total should be(2)
      paged.results(0)(name) should be("John Doe")
      paged.results(0).score should be(1.7996649742126465)
      paged.results(1)(name) should be("Jane Doe")
      paged.results(1).score should be(1.33756685256958)
    }
    "query fuzzy matching john and jane with highlighting" in {
      val paged = lucene.query().scoreDocs().sort(Sort.Score).filter(fuzzy(name("jhn"))).highlight().search()
      paged.total should be(2)
      paged.results(0)(name) should be("John Doe")
      paged.results(0).score should be(0.9241962432861328)
      val highlightsJohn = paged.results(0).highlighting(name)
      highlightsJohn.length should be(1)
      highlightsJohn.head.fragment should be("<em>John</em> Doe")
      highlightsJohn.head.word should be("John")

      paged.results(1)(name) should be("Jane Doe")
      paged.results(1).score should be(0.4620981216430664)
      val highlightsJane = paged.results(1).highlighting(name)
      highlightsJane.length should be(1)
      highlightsJane.head.fragment should be("<em>Jane</em> Doe")
      highlightsJane.head.word should be("Jane")
    }
    "query fuzzy matching john and jane with highlighting in full text search" in {
      val paged = lucene.query().scoreDocs().sort(Sort.Score).filter(fuzzy(lucene.fullText("jhn"))).highlight().search()
      paged.total should be(2)
      paged.results(0)(name) should be("John Doe")
      paged.results(0).score should be(0.7261541485786438)
      val highlightsJohn = paged.results(0).highlighting(name)
      highlightsJohn.length should be(1)
      highlightsJohn.head.fragment should be("<em>John</em> Doe")
      highlightsJohn.head.word should be("John")

      paged.results(1)(name) should be("Jane Doe")
      paged.results(1).score should be(0.4959101378917694)
      val highlightsJane = paged.results(1).highlighting(name)
      highlightsJane.length should be(1)
      highlightsJane.head.fragment should be("<em>Jane</em> Doe")
      highlightsJane.head.word should be("Jane")
    }
    "query all keywords from keyword indexing" in {
      val keywords = keywordIndexing.search(limit = 20)
      keywords.results.map(_.word) should be(List("true", "123456789", "0.123", "23", "John", "21", "Jane", "Doe", "31", "Andrew", "Anderson", "25", "Billy", "Bob", "19", "Carly", "Charles"))
      keywords.total should be(17)
      keywords.maxScore should be(1.0)
      keywords.results.length should be(17)
    }
    "query keywords filtered by 'doe'" in {
      val keywords = keywordIndexing.search("do*")
      keywords.results.map(_.word) should be(List("Doe"))
      keywords.total should be(1)
      keywords.maxScore should be(1.0)
      keywords.results.length should be(1)
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
    "query more like this" in {
      val paged = lucene.query().filter(mlt(name("Doe"))).search()
      paged.total should be(0)
    }
    "query more like this with overloaded config" in {
      val paged = lucene.query().filter(mlt(name("Doe"), minTermFreq = 0, minDocFreq = 0)).search()
      paged.total should be(2)
      paged.results(0)(name) should be("John Doe")
      paged.results(1)(name) should be("Jane Doe")
    }
    "query more like this for full text " in {
      val paged = lucene.query().filter(mltFullText("John Doe", minTermFreq = 0, minDocFreq = 0)).search()
      paged.total should be(2)
      paged.results(0)(name) should be("John Doe")
      paged.results(1)(name) should be("Jane Doe")
    }
    "delete John Doe" in {
      lucene.query().search().results.length should be(6)
      lucene.delete(term(name("john")))
      lucene.query().search().results.length should be(5)
    }
    "update Jane Doe with name and age" in {
      val results = lucene.query().search().results
      results.length should be(5)

      val janeDoe = results.find(_.apply(name) == "Jane Doe").getOrElse(fail("Cannot find Jane Doe in results"))
      janeDoe.update.fields(name("Janie Doe")).index()

      val results2 = lucene.query().filter(exact(name("Janie Doe"))).search()
      results2.total should be(1)
      val result = results2.entries.head
      result(name) should be("Janie Doe")
      result(age) should be(21)
    }
    // TODO: storage and querying of Array[Byte]
    "dispose" in {
      lucene.dispose()
    }
  }
}
