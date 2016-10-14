package tests

import com.outr.lucene4s.Lucene
import com.outr.lucene4s.field.Field
import com.outr.lucene4s.mapper.Searchable
import org.scalatest.{Matchers, WordSpec}

class SearchableSpec extends WordSpec with Matchers {
  "Searchable" should {
    "insert a Person" in {
      lucene.person.insert(Person("John", "Doe", "123 Somewhere Rd.", "Lalaland", "California", "12345"))
    }
    "verify the record exists" in {
      val paged = lucene.query().search()
      paged.total should be(1)
      val result = paged.results.head
      result(lucene.person.firstName) should be("John")
      result(lucene.person.lastName) should be("Doe")
    }
    "query back the record as a Person" in {
      val paged = lucene.query().search()
      paged.total should be(1)
      val result = paged.results.head
      val john = lucene.person(result)
      john should be(Person("John", "Doe", "123 Somewhere Rd.", "Lalaland", "California", "12345"))
    }
    "update the record" in {
      val john = Person("John", "Doe", "321 Nowhere St.", "Lalaland", "California", "12345")
      lucene.person.update(john)
    }
    "query back the record as a Person verifying the update" in {
      val paged = lucene.query().search()
      paged.total should be(1)
      val result = paged.results.head
      val john = lucene.person(result)
      john should be(Person("John", "Doe", "321 Nowhere St.", "Lalaland", "California", "12345"))
    }
    "delete the record" in {
      val john = Person("John", "Doe", "321 Nowhere St.", "Lalaland", "California", "12345")
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
    override def idFields: List[Field[_]] = List(firstName, lastName)

    // Create method stubs for code completion
    def firstName: Field[String]
    def lastName: Field[String]
  }

  case class Person(firstName: String, lastName: String, address: String, city: String, state: String, zip: String)
}