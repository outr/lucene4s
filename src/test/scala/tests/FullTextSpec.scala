package tests

import com.outr.lucene4s._
import com.outr.lucene4s.query.Sort
import org.scalatest.{Matchers, WordSpec}

class FullTextSpec extends WordSpec with Matchers {
  val lucene = new Lucene(defaultFullTextSearchable = true)   // Default all fields to be fullTextSearchable
  val firstName = lucene.create.field[String]("firstName")
  val lastName = lucene.create.field[String]("lastName")
  val age = lucene.create.field[Int]("age")
  val company = lucene.create.field[String]("company")

  "FullText" should {
    "index a few documents" in {
      lucene.doc().fields(firstName("John"), lastName("Doe"), age(23), company("Doeco")).index()
      lucene.doc().fields(firstName("Jane"), lastName("Doe"), age(21), company("Superstore")).index()
      lucene.doc().fields(firstName("Baby"), lastName("Doe"), age(1)).index()
      lucene.doc().fields(firstName("James"), lastName("Ray"), age(28), company("Doeco")).index()
      lucene.doc().fields(firstName("Amy"), lastName("Ray"), age(29), company("Buymore")).index()
    }
    "search by last name" in {
      val paged = lucene.query().filter("doe").search()
      paged.total should be(3)
    }
    "search by age" in {
      val paged = lucene.query().filter(age(21)).search()
      paged.total should be(1)
    }
    "search full text for 'doe'" in {
      val paged = lucene.query().sort(Sort.Score).filter(wildcard("doe*")).search()
      paged.total should be(4)
      paged.results(0)(firstName) should be("John")
      paged.results(1)(firstName) should be("Jane")
      paged.results(2)(firstName) should be("Baby")
      paged.results(3)(firstName) should be("James")
    }
    "search full text for '21'" in {
      val paged = lucene.query().filter(term("21")).search()
      paged.total should be(1)
      paged.results(0)(firstName) should be("Jane")
    }
  }
}
