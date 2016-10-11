package tests

import com.outr.lucene4s._
import org.scalatest.{Matchers, WordSpec}

class SimpleSpec extends WordSpec with Matchers {
  val lucene = new Lucene()

  "Simple Spec" should {
    "create a simple index" in {
      lucene.doc().field("name", "John Doe").index()
    }
    "query for the index" in {
      val searchResults = lucene.query("name").search("john")
      val results = searchResults.results
      results.length should be(1)
      results(0).string("name") should be("John Doe")
    }
    "dispose" in {
      lucene.dispose()
    }
  }
}
