package tests

import com.outr.lucene4s._
import com.outr.lucene4s.field.Field
import com.outr.lucene4s.mapper.Searchable
import com.outr.lucene4s.query.{Condition, SearchTerm}
import org.scalatest.{Matchers, WordSpec}

class SearchableSpec extends WordSpec with Matchers {
  "Searchable" should {
    "validate the generated docType" in {
      lucene.person.docTypeName should be("tests.SearchableSpec.Person")
      lucene.person.docType shouldNot be(null)
    }
    "insert a Person" in {
      val john = Person(1, "John", "Doe", 23, "123 Somewhere Rd.", "Lalaland", "California", "12345")
      lucene.person.insert(john).facets(
        lucene.person.tags("monkey"),
        lucene.person.tags("elephant")
      ).index()
    }
    "verify the record exists" in {
      val paged = lucene.query().facet(lucene.person.tags, limit = 10).search()
      paged.total should be(1)
      val result = paged.results.head
      result(lucene.person.firstName) should be("John")
      result(lucene.person.lastName) should be("Doe")
      result(lucene.person.docType) should be(lucene.person.docTypeName)
      val tags = paged.facet(lucene.person.tags).get
      tags.childCount should be(2)
      tags.totalCount should be(2)
      tags.values(0).count should be(1)
      tags.values(0).value should be("monkey")
      tags.values(1).count should be(1)
      tags.values(1).value should be("elephant")
    }
    "query back the record as a Person" in {
      val paged = lucene.query().search()
      paged.total should be(1)
      val result = paged.results.head
      val john = lucene.person(result)
      john should be(Person(1, "John", "Doe", 23, "123 Somewhere Rd.", "Lalaland", "California", "12345"))
    }
    "query back the record as a Person using Searchable.query" in {
      val paged = lucene.person.query().search()
      paged.total should be(1)
      val john = paged.entries.head
      john should be(Person(1, "John", "Doe", 23, "123 Somewhere Rd.", "Lalaland", "California", "12345"))
    }
    "update the record" in {
      val john = Person(1, "John", "Doe", 23, "321 Nowhere St.", "Lalaland", "California", "12345")
      lucene.person.update(john).index()
    }
    "query back the record as a Person verifying the update" in {
      val paged = lucene.query().search()
      paged.total should be(1)
      val result = paged.results.head
      val john = lucene.person(result)
      john should be(Person(1, "John", "Doe", 23, "321 Nowhere St.", "Lalaland", "California", "12345"))
    }
    "delete the record" in {
      val john = Person(1, "John", "Doe", 23, "321 Nowhere St.", "Lalaland", "California", "12345")
      lucene.person.delete(john)
    }
    "query and make sure the record was deleted" in {
      val paged = lucene.query().search()
      paged.total should be(0)
    }
  }

  object lucene extends Lucene {
    val person = create.searchable[SearchablePerson]
  }

  trait SearchablePerson extends Searchable[Person] {
    // We must implement the criteria for updating and deleting
    override def idSearchTerm(t: Person): SearchTerm = exact(id(t.id))

    // Create method stubs for code completion
    def id: Field[Int]
    def firstName: Field[String]
    def lastName: Field[String]

    val tags = lucene.create.facet("tags", multiValued = true)
  }

  case class Person(id: Int, firstName: String, lastName: String, age: Int, address: String, city: String, state: String, zip: String)
}