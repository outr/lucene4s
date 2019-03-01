package tests

import com.outr.lucene4s._
import com.outr.lucene4s.facet.FacetField
import com.outr.lucene4s.field.Field
import com.outr.lucene4s.query.Condition
import org.scalatest.{Matchers, WordSpec}

class FacetsSpec extends WordSpec with Matchers {
  val lucene: Lucene = new DirectLucene(uniqueFields = List("name"))
  val name: Field[String] = lucene.create.field[String]("name")
  val author: FacetField = lucene.create.facet("Author", multiValued = true)
  val keywords: FacetField = lucene.create.facet("Keywords", multiValued = true)
  val publishDate: FacetField = lucene.create.facet("Publish Date", hierarchical = true)

  "Facets" should {
    "create a few faceted documents" in {
      lucene.doc().fields(name("One")).facets(
        author("Bob"),
        author("James"),
//        keywords("support@one.com"),
//        keywords("support@two.com"),
        publishDate("2010", "10", "15")
      ).index()
      lucene.doc().fields(name("Two")).facets(
        author("Lisa"),
//        keywords("support@one.com"),
        publishDate("2010", "10", "20")
      ).index()
      lucene.doc().fields(name("Three")).facets(
        author("Lisa"),
//        keywords("support@two.com"),
        publishDate("2012", "1", "1")
      ).index()
      lucene.doc().fields(name("Four")).facets(
        author("Susan"),
//        keywords("support@three.com"),
        publishDate("2012", "1", "7")
      ).index()
      lucene.doc().fields(name("Five")).facets(
        author("Frank"),
        publishDate("1999", "5", "5")
      ).index()
      lucene.doc().fields(name("Six")).facets(
        author("George"),
        publishDate("1999")
      ).index()
      lucene.doc().fields(name("Seven")).facets(
        author("Bob"),
        publishDate()
      ).index()
    }
    "list all author facets" in {
      val page = lucene.query().limit(10).facet(author).search()
      page.facet(publishDate) should be(None)
      val authorResult = page.facet(author).get
      authorResult.childCount should be(6)
      authorResult.totalCount should be(8)
      authorResult.values.map(_.value) should be(Vector("Bob", "Lisa", "James", "Susan", "Frank", "George"))
      authorResult.values.map(_.count) should be(Vector(2, 2, 1, 1, 1, 1))
      page.results.map(_(name)) should be(Vector("One", "Two", "Three", "Four", "Five", "Six", "Seven"))
    }
    "list all publishDate facets" in {
      val page = lucene.query().limit(10).facet(publishDate).search()
      page.facet(author) should be(None)
      val publishResult = page.facet(publishDate).get
      publishResult.childCount should be(3)
      publishResult.totalCount should be(6)
      publishResult.values.map(_.value) should be(Vector("2010", "2012", "1999"))
      publishResult.values.map(_.count) should be(Vector(2, 2, 2))
      page.results.map(_(name)) should be(Vector("One", "Two", "Three", "Four", "Five", "Six", "Seven"))
    }
//    "list all support@one.com keyword facets" in {
//      val page = lucene.query().filter(drillDown(keywords("support@one.com"))).search()
//      val names = page.entries.map(r => r(name))
//      names should be(Vector("One", "Two"))
//    }
    "modify a record" in {
      val page = lucene.query().filter(exact(name("Five"))).search()
      page.results.length should be(1)
      val result = page.results.head
      result.update.fields(name("Cinco")).index()
    }
    "list all results for 2010" in {
      val page = lucene
        .query()
        .limit(10)
        .facet(author)
        .facet(publishDate, path = List("2010"))
        .filter(drillDown(publishDate("2010")))
        .search()
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
    "exclude all results for 2010" in {
      val page = lucene
        .query()
        .limit(10)
        .facet(author)
        .facet(publishDate)
        .filter(grouped(
          matchAll() -> Condition.Must,
          drillDown(publishDate("2010")) -> Condition.MustNot
        ))
        .search()
      val authorResult = page.facet(author).get
      authorResult.childCount should be(5)
      authorResult.totalCount should be(5)
      authorResult.values.map(_.value) should be(Vector("Bob", "Lisa", "Susan", "Frank", "George"))
      authorResult.values.map(_.count) should be(Vector(1, 1, 1, 1, 1))
      val publishResult = page.facet(publishDate).get
      publishResult.childCount should be(2)
      publishResult.totalCount should be(4)
      publishResult.values.map(_.value) should be(Vector("2012", "1999"))
      publishResult.values.map(_.count) should be(Vector(2, 2))
      page.results.map(_(name)) should be(Vector("Three", "Four", "Six", "Seven", "Cinco"))
    }
    "list all results for 2010/10" in {
      val page = lucene
        .query()
        .limit(10)
        .facet(author)
        .facet(publishDate, path = List("2010", "10"))
        .filter(drillDown(publishDate("2010", "10")))
        .search()
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
      val page = lucene
        .query()
        .limit(10)
        .facet(author)
        .facet(publishDate, path = List("2010", "10", "20"))
        .filter(drillDown(publishDate("2010", "10", "20")))
        .search()
      val authorResult = page.facet(author).get
      authorResult.childCount should be(1)
      authorResult.totalCount should be(1)
      authorResult.values.map(_.value) should be(Vector("Lisa"))
      authorResult.values.map(_.count) should be(Vector(1))
      val publishResult = page.facet(publishDate).get
      publishResult.values should be(Vector.empty)
      publishResult.childCount should be(0)
      publishResult.totalCount should be(0)
      page.results.map(_(name)) should be(Vector("Two"))
    }
    "show only results for 1999" in {
      val page = lucene
        .query()
        .limit(10)
        .facet(author)
        .facet(publishDate, path = List("1999"))
        .filter(drillDown(publishDate("1999"), onlyThisLevel = true))
        .search()
      val authorResult = page.facet(author).get
      authorResult.childCount should be(1)
      authorResult.totalCount should be(1)
      authorResult.values.map(_.value) should be(Vector("George"))
      authorResult.values.map(_.count) should be(Vector(1))
      val publishResult = page.facet(publishDate).get
      publishResult.childCount should be(0)
      publishResult.totalCount should be(0)
      page.results.map(_(name)) should be(Vector("Six"))
    }
    "show only top-level results without a publish date" in {
      val page = lucene
        .query()
        .limit(10)
        .facet(author)
        .facet(publishDate)
        .filter(drillDown(publishDate(), onlyThisLevel = true))
        .search()
      val authorResult = page.facet(author).get
      authorResult.childCount should be(1)
      authorResult.totalCount should be(1)
      authorResult.values.map(_.value) should be(Vector("Bob"))
      authorResult.values.map(_.count) should be(Vector(1))
      val publishResult = page.facet(publishDate).get
      publishResult.childCount should be(0)
      publishResult.totalCount should be(0)
      page.results.map(_(name)) should be(Vector("Seven"))
    }
    "delete a faceted document" in {
      lucene.delete(term(name("Four")))
      lucene.commit()
    }
    "query excluding the deleted document" in {
      val page = lucene.query().limit(10).facet(author).search()
      page.facet(publishDate) should be(None)
      val authorResult = page.facet(author).get
      authorResult.values.map(_.value) should be(Vector("Bob", "Lisa", "James", "Frank", "George"))
      authorResult.values.map(_.count) should be(Vector(2, 2, 1, 1, 1))
      page.results.map(_(name)) should be(Vector("One", "Two", "Three", "Six", "Seven", "Cinco"))
    }
  }
}