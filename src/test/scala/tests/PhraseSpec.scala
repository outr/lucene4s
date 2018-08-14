package tests

import com.outr.lucene4s._
import com.outr.lucene4s.field.Field
import com.outr.lucene4s.query.SearchTerm
import org.scalatest.{Matchers, WordSpec}

class PhraseSpec extends WordSpec with Matchers {
  val lucene = new DirectLucene(uniqueFields = List("name"), autoCommit = true)
  val name: Field[String] = lucene.create.field[String]("name")

  "Phrase Spec" should {
    "create sample names" in {
      lucene.doc().fields(name("John Doe")).index()
      lucene.doc().fields(name("Jane Doe")).index()
      lucene.doc().fields(name("James Dean Doe")).index()
      lucene.doc().fields(name("Bob Dean Doe")).index()
      lucene.doc().fields(name("Dean James Doe")).index()
      lucene.doc().fields(name("John James Dean Doe")).index()
      lucene.doc().fields(name("James Dean")).index()
      lucene.doc().fields(name("Jimmy James Dean")).index()
      lucene.doc().fields(name("Doe Dean James")).index()
    }
    "search for 'James Doe' by phrase with slop=1" in {
      val names = namesFor(phrase(name("James Doe"), slop = 1))
      names should be(Vector("Dean James Doe", "James Dean Doe", "John James Dean Doe"))
    }
    "search for 'James Doe' by phrase with slop=0" in {
      val names = namesFor(phrase(name("James Doe"), slop = 0))
      names should be(Vector("Dean James Doe"))
    }
    "search exact match 'James Dean Doe'" in {
      val names = namesFor(phrase(name("James Dean Doe")))
      names should be(Vector("James Dean Doe", "John James Dean Doe"))
    }
    "search exact match 'Dean James'" in {
      val names = namesFor(phrase(name("Dean James")))
      names should be(Vector("Dean James Doe", "Doe Dean James"))
    }
    "search exact match 'Dean Doe James'" in {
      val names = namesFor(phrase(name("Dean Doe James")))
      names should be(Vector.empty)
    }
  }

  private def namesFor(terms: SearchTerm*): Vector[String] = {
    lucene.query().filter(terms: _*).limit(100).search().results.map(_(name))
  }
}
