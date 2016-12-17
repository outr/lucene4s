package tests

import com.outr.lucene4s._
import com.outr.lucene4s.field.Field
import com.outr.lucene4s.field.value.SpatialPoint
import com.outr.lucene4s.query.Sort
import org.scalatest.{Matchers, WordSpec}

import squants.space.LengthConversions._

class SpatialSpec extends WordSpec with Matchers {
  val lucene = new Lucene()
  val name: Field[String] = lucene.create.field[String]("name")
  val location: Field[SpatialPoint] = lucene.create.field[SpatialPoint]("location")
  val city: Field[String] = lucene.create.field[String]("city")

  "Spatial Spec" should {
    val newYorkCity = SpatialPoint(40.7142, -74.0119)
    val chicago = SpatialPoint(41.8119,	-87.6873)
    val jeffersonValley = SpatialPoint(41.3385, -73.7947)
    val noble = SpatialPoint(35.1417, -97.3409)
    val oklahomaCity = SpatialPoint(35.5514, -97.4075)
    val yonkers = SpatialPoint(40.9461, -73.8669)

    "create a few spatial documents" in {
      lucene.doc().fields(name("Mikey"), location(newYorkCity), city("New York City")).index()
      lucene.doc().fields(name("Johnny"), location(chicago), city("Chicago")).index()
      lucene.doc().fields(name("Jeff"), location(jeffersonValley), city("Jefferson Valley")).index()
      lucene.doc().fields(name("Joe"), location(noble), city("Noble")).index()
    }
    "query all the documents sorted from a point in OKC" in {
      val paged = lucene.query().sort(Sort.nearest(location, oklahomaCity)).search()
      paged.total should be(4)
      val results = paged.results
      results.length should be(4)
      results(0)(name) should be("Joe")
      results(0)(location) should be(noble)
      results(0)(city) should be("Noble")
    }
    "query all the documents sorted from a point in Yonkers" in {
      val paged = lucene.query().sort(Sort.nearest(location, yonkers)).search()
      paged.total should be(4)
      val results = paged.results
      results.length should be(4)
      results(0)(name) should be("Mikey")
      results(0)(location) should be(newYorkCity)
      results(0)(city) should be("New York City")
      results(1)(name) should be("Jeff")
      results(1)(location) should be(jeffersonValley)
      results(1)(city) should be("Jefferson Valley")
    }
    "query all documents within a 50 mile radius of a point in New York City" in {
      val paged = lucene
        .query()
        .filter(spatialDistance(location, newYorkCity, 50.miles))
        .sort(Sort.nearest(location, newYorkCity))
        .search()
      paged.total should be(2)
      val results = paged.results
      results.length should be(2)
      results(0)(name) should be("Mikey")
      results(1)(name) should be("Jeff")
    }
  }
}