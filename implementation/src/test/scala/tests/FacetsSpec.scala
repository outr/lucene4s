package tests

import com.outr.lucene4s._
import com.outr.lucene4s.facet.FacetField
import com.outr.lucene4s.field.Field
import com.outr.lucene4s.query.Condition
import org.apache.lucene.facet.FacetsCollectorManager
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.MatchAllDocsQuery
import org.apache.lucene.store.Directory
import org.apache.lucene.store.NIOFSDirectory
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.nio.file.Paths

class FacetsSpec extends AnyWordSpec with Matchers with BeforeAndAfter {
  val lucene: Lucene = new DirectLucene(uniqueFields = List("name"))
  val name: Field[String] = lucene.create.field[String]("name")
  val author: FacetField = lucene.create.facet("Author", multiValued = true)
  val keywords: Field[List[String]] = lucene.create.field[List[String]]("Keywords")
  val keywordsFacet: FacetField = lucene.create.facet("Keywords", multiValued = true)
  val publishDate: FacetField = lucene.create.facet("Publish Date", hierarchical = true)

  before {

      lucene.doc().fields(name("One")).facets(
        author("Bob"),
        author("James"),
        keywordsFacet("support@one.com"),
        keywordsFacet("support@two.com"),
        publishDate("2010", "10", "15")
      ).index()
      lucene.doc().fields(name("Two")).facets(
        author("Lisa"),
        keywordsFacet("support@one.com"),
        publishDate("2010", "10", "20")
      ).index()
      lucene.doc().fields(name("Three")).facets(
        author("Lisa"),
        keywordsFacet("support@two.com"),
        publishDate("2012", "1", "1")
      ).index()
      lucene.doc().fields(name("Four")).facets(
        author("Susan"),
        keywordsFacet("support@three.com"),
        publishDate("2012", "1", "7")
      ).index()
      lucene.doc().fields(name("Five")).facets(
        author("Frank"),
        keywordsFacet("support"),
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

  after {
    lucene.deleteAll()
  }

  "Facets" should {
    "create a few faceted documents" in {
    }
    "list all author facets" in {
      val page = lucene.query().limit(10).facet(author).search()
      println(page.results)
      page.facet(publishDate) should be(None)
      val authorResult = page.facet(author).get
      authorResult.childCount should be(6)
      authorResult.totalCount should be(8)
      authorResult.values.map(_.value) should be(Vector("Bob", "Lisa", "James", "Susan", "Frank", "George"))
      authorResult.values.map(_.count) should be(Vector(2, 2, 1, 1, 1, 1))
      val bob = page.results.head
      bob(keywords).toSet should be(Set("support@one.com", "support@two.com"))
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
    "list all support@one.com keyword facets" in {
      val page = lucene.query().filter(drillDown(keywordsFacet("support@one.com"))).search()
      val names = page.entries.map(r => r(name))
      names should be(Vector("One", "Two"))
    }
    "modify a record" in {
      val page = lucene.query().filter(exact(name("Five"))).search()
      page.results.length should be(1)
      val result = page.results.head
      result.update.clear(name).fields(name("Cinco")).index()
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
      authorResult.values.map(_.value).toSet should be(Set("Bob", "James", "Lisa"))
      authorResult.values.map(_.count) should be(Vector(1, 1, 1))
      val publishResult = page.facet(publishDate).get
      publishResult.childCount should be(1)
      publishResult.totalCount should be(2)
      publishResult.values.map(_.value) should be(Vector("10"))
      publishResult.values.map(_.count) should be(Vector(2))
      page.results.map(_(name)).toSet should be(Set("One", "Two"))
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
      page.results.map(_(name)) should be(Vector("Three", "Four", "Five", "Six", "Seven"))
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
      authorResult.values.map(_.value).toSet should be(Set("Bob", "James", "Lisa"))
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
    "show all results for support@two.com" in {
      val page = lucene
        .query()
        .limit(10)
        .filter(drillDown(keywordsFacet("support@two.com")))
        .search()
      page.results.map(_(name)) should be(Vector("One", "Three"))
    }
    "show all results for support@three.com or support" in {
      val page = lucene
        .query()
        .filter(any(drillDown(keywordsFacet("support@three.com")), drillDown(keywordsFacet("support"))))
        .search()
      page.results.map(_(name)).toSet should be(Set("Four", "Five"))
    }
    "remove a keyword from One" in {
      val page = lucene.query().filter(name("One")).search()
      page.total should be(1)
      page.results.head.update.remove(keywordsFacet("support@two.com")).index()
      lucene.commit()

      lucene
        .query()
        .limit(10)
        .filter(drillDown(keywordsFacet("support@two.com")))
        .search()
        .results.map(_(name)) should be(Vector("Three"))
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
      authorResult.totalCount should be(1)
      authorResult.childCount should be(1)
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
      
      val page = lucene.query().limit(10).facet(author).search()
      
      page.facet(publishDate) should be(None)
      val authorResult = page.facet(author).get
      authorResult.values.map(_.value) should be(Vector("Bob", "Lisa", "James", "Frank", "George"))
      authorResult.values.map(_.count) should be(Vector(2, 2, 1, 1, 1))
      page.results.map(_(name)).toSet should be(Set("One", "Two", "Three", "Six", "Seven", "Five"))
    }
  }
}