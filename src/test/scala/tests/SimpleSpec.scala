package tests

import com.outr.lucene4s._
import org.scalatest.{Matchers, WordSpec}

class SimpleSpec extends WordSpec with Matchers {
  val lucene = new Lucene()

  "Simple Spec" should {
    "create a simple document" in {
      lucene.doc().field("name", "John Doe").index()
    }
    "query for the index" in {
      val paged = lucene.query("name").search("john")
      paged.total should be(1)
      val results = paged.results
      results.length should be(1)
      results(0).string("name") should be("John Doe")
    }
    "add a few more documents" in {
      lucene.doc().field("name", "Jane Doe").index()
      lucene.doc().field("name", "Andrew Anderson").index()
      lucene.doc().field("name", "Billy Bob").index()
      lucene.doc().field("name", "Carly Charles").index()
      lucene.flush()
    }
    "query using pagination" in {
      val paged = lucene.query("name").limit(2).search("*:*")
      paged.total should be(5)
//      val results = paged.results
//      results.length should be(2)
//      results(0).string("name") should be("John Doe")
    }
    "dispose" in {
      lucene.dispose()
    }
  }
}
