package tests

import com.outr.lucene4s.Lucene
import org.scalatest.{Matchers, WordSpec}

class FacetsSpec extends WordSpec with Matchers {
  val lucene = new Lucene()
  val name = lucene.create.field[String]("name")
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
      val page = lucene.query(name).limit(10).facet(author).search()
      page.facet(publishDate) should be(None)
      val authorResult = page.facet(author).get
      authorResult.childCount should be(4)
      authorResult.count should be(5)
      authorResult.values.map(_.value).toSet should be(Set("Bob", "Lisa", "Susan", "Frank"))
      authorResult.values.map(_.count).toSet should be(Set(1, 2, 1, 1))
    }
    "list all publishDate facets" in {
      val page = lucene.query(name).limit(10).facet(publishDate).search()
      page.facet(author) should be(None)
      val publishResult = page.facet(publishDate).get
      publishResult.childCount should be(3)
      publishResult.count should be(5)
      publishResult.values.map(_.value).toSet should be(Set("2010", "2012", "1999"))
      publishResult.values.map(_.count).toSet should be(Set(2, 2, 1))
    }
    "list all results for 2010" in {
      val page = lucene.query(name).limit(10).facet(author).facet(publishDate, path = List("2010")).search()
      val authorResult = page.facet(author).get
      authorResult.childCount should be(2)
      authorResult.count should be(2)
      authorResult.values.map(_.value).toSet should be(Set("Bob", "Lisa"))
      authorResult.values.map(_.count).toSet should be(Set(1, 1))
    }
  }
}