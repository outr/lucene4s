package tests

import com.outr.lucene4s.Lucene
import org.scalatest.{Matchers, WordSpec}

class FacetsSpec extends WordSpec with Matchers {
  val lucene = new Lucene()
  val author = lucene.create.facet("Author")
  val publishDate = lucene.create.facet("Publish Date", hierarchical = true)

  "Facets" should {
    "create a few faceted documents" in {
      lucene.doc().add(author("Bob")).add(publishDate("2010", "10", "15")).index()
      lucene.doc().add(author("Lisa")).add(publishDate("2010", "10", "20")).index()
      lucene.doc().add(author("Lisa")).add(publishDate("2012", "1", "1")).index()
      lucene.doc().add(author("Susan")).add(publishDate("2012", "1", "7")).index()
      lucene.doc().add(author("Frank")).add(publishDate("1999", "5", "5")).index()
    }
    "list all author facets" in {

    }
  }
}