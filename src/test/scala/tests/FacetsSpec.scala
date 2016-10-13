package tests

import com.outr.lucene4s.Lucene
import org.scalatest.{Matchers, WordSpec}

class FacetsSpec extends WordSpec with Matchers {
  val lucene = new Lucene()
  val name = lucene.create.field[String]("name")
  val author = lucene.create.facet("Author", multiValued = true)
  val publishDate = lucene.create.facet("Publish Date", hierarchical = true)

  "Facets" should {
    "create a few faceted documents" in {
      lucene.doc().fields(name("One")).facets(author("Bob"), author("James"), publishDate("2010", "10", "15")).index()
      lucene.doc().fields(name("Two")).facets(author("Lisa"), publishDate("2010", "10", "20")).index()
      lucene.doc().fields(name("Three")).facets(author("Lisa"), publishDate("2012", "1", "1")).index()
      lucene.doc().fields(name("Four")).facets(author("Susan"), publishDate("2012", "1", "7")).index()
      lucene.doc().fields(name("Five")).facets(author("Frank"), publishDate("1999", "5", "5")).index()
    }
    "list all author facets" in {
      val page = lucene.query().limit(10).facet(author).search()
      page.facet(publishDate) should be(None)
      val authorResult = page.facet(author).get
      authorResult.childCount should be(5)
      authorResult.totalCount should be(6)
      authorResult.values.map(_.value) should be(Vector("Lisa", "Bob", "James", "Susan", "Frank"))
      authorResult.values.map(_.count) should be(Vector(2, 1, 1, 1, 1))
      page.results.map(_(name)) should be(Vector("One", "Two", "Three", "Four", "Five"))
    }
    "list all publishDate facets" in {
      val page = lucene.query().limit(10).facet(publishDate).search()
      page.facet(author) should be(None)
      val publishResult = page.facet(publishDate).get
      publishResult.childCount should be(3)
      publishResult.totalCount should be(5)
      publishResult.values.map(_.value) should be(Vector("2010", "2012", "1999"))
      publishResult.values.map(_.count) should be(Vector(2, 2, 1))
      page.results.map(_(name)) should be(Vector("One", "Two", "Three", "Four", "Five"))
    }
    "list all results for 2010" in {
      val page = lucene.query().limit(10).facet(author).facet(publishDate, path = List("2010")).search()
      val authorResult = page.facet(author).get
      authorResult.childCount should be(3)
      authorResult.totalCount should be(3)
      authorResult.values.map(_.value) should be(Vector("Bob", "James", "Lisa"))
      authorResult.values.map(_.count) should be(Vector(1, 1, 1))
      val publishResult = page.facet(publishDate).get
      publishResult.childCount should be(1)
      publishResult.totalCount should be(2)
      publishResult.values.map(_.value) should be(Vector("10"))
      publishResult.values.map(_.count) should be(Vector(2))
      page.results.map(_(name)) should be(Vector("One", "Two"))
    }
    "list all results for 2010/10" in {
      val page = lucene.query().limit(10).facet(author).facet(publishDate, path = List("2010", "10")).search()
      val authorResult = page.facet(author).get
      authorResult.childCount should be(3)
      authorResult.totalCount should be(3)
      authorResult.values.map(_.value) should be(Vector("Bob", "James", "Lisa"))
      authorResult.values.map(_.count) should be(Vector(1, 1, 1))
      val publishResult = page.facet(publishDate).get
      publishResult.childCount should be(2)
      publishResult.totalCount should be(2)
      publishResult.values.map(_.value) should be(Vector("15", "20"))
      publishResult.values.map(_.count) should be(Vector(1, 1))
      page.results.map(_(name)) should be(Vector("One", "Two"))
    }
    "list all results for 2010/10/20" in {
      val page = lucene.query().limit(10).facet(author).facet(publishDate, path = List("2010", "10", "20")).search()
      val authorResult = page.facet(author).get
      authorResult.childCount should be(1)
      authorResult.totalCount should be(1)
      authorResult.values.map(_.value) should be(Vector("Lisa"))
      authorResult.values.map(_.count) should be(Vector(1))
      val publishResult = page.facet(publishDate).get
      publishResult.childCount should be(0)
      publishResult.totalCount should be(0)
      page.results.map(_(name)) should be(Vector("Two"))
    }
  }
}