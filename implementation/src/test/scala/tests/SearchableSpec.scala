package tests

import com.outr.lucene4s._
import com.outr.lucene4s.facet.FacetField
import com.outr.lucene4s.field.{Field, FieldType}
import com.outr.lucene4s.keyword.KeywordIndexing
import com.outr.lucene4s.mapper.Searchable
import com.outr.lucene4s.query.{Condition, SearchTerm}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SearchableSpec extends AnyWordSpec with Matchers {
  "Searchable" should {
    "validate the generated docType" in {
      lucene.person.docTypeName should be("testsSearchableSpecPerson")
      lucene.person.docType shouldNot be(null)
    }
    "insert a Person" in {
      val john = Person(Id[Person]("1"), "John", "Doe", 23, "123 Somewhere Rd.", "Lalaland", "California", "12345", List("john", "johnny"))
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
      john should be(Person(Id[Person]("1"), "John", "Doe", 23, "123 Somewhere Rd.", "Lalaland", "California", "12345", List("john", "johnny")))
    }
    "query back the record as a Person using Searchable.query" in {
      val paged = lucene.person.query().search()
      paged.total should be(1)
      val john = paged.entries.head
      john should be(Person(Id[Person]("1"), "John", "Doe", 23, "123 Somewhere Rd.", "Lalaland", "California", "12345", List("john", "johnny")))
    }
    "insert another Person" in {
      val jane = Person(Id[Person]("2"), "Jane", "Doe", 21, "123 Somewhere Rd.", "Lalaland", "California", "12345", List("jane"))
      lucene.person.insert(jane).facets(
        lucene.person.tags("cheetah"),
        lucene.person.tags("dolphin")
      ).index()
    }
    "query back last name removing duplicates" in {
      val lastNames = lucene.lastNameKeywords.search("doe")
      lastNames.results.map(_.word) should be(List("Doe"))
      lastNames.results.map(_.highlighted("[", "]")) should be(List("[Doe]"))
      lastNames.total.value should be(1)
    }
    "query back last name on partial" in {
      val lastNames = lucene.lastNameKeywords.search("(*:* AND do*)")
      lastNames.results.map(_.word) should be(List("Doe"))
      lastNames.results.map(_.highlighted("[", "]")) should be(List("[Do]e"))
      lastNames.total.value should be(1)
    }
    "verify only last names in keywords" in {
      val lastNames = lucene.lastNameKeywords.search("john")
      lastNames.total.value should be(0)
    }
    "delete the new record" in {
      val jane = Person(Id[Person]("2"), "Jane", "Doe", 21, "321 Nowhere St.", "Lalaland", "California", "12345", List("jane"))
      lucene.person.delete(jane)
    }
    "update the record" in {
      val john = Person(Id[Person]("1"), "John", "Doe", 23, "321 Nowhere St.", "Lalaland", "California", "12345", List("john", "johnny"))
      lucene.person.update(john).index()
      lucene.commit()
    }
    "query back the record as a Person verifying the update" in {
      val paged = lucene.query().search()
      paged.total should be(1)
      val result = paged.results.head
      val john = lucene.person(result)
      john should be(Person(Id[Person]("1"), "John", "Doe", 23, "321 Nowhere St.", "Lalaland", "California", "12345", List("john", "johnny")))
    }
    "delete the record" in {
      val john = Person(Id[Person]("1"), "John", "Doe", 23, "321 Nowhere St.", "Lalaland", "California", "12345", List("john", "johnny"))
      lucene.person.delete(john)
      lucene.commit()
    }
    "query and make sure the record was deleted" in {
      val paged = lucene.query().search()
      paged.total should be(0)
    }
  }

  object lucene extends DirectLucene(uniqueFields = List("id")) {
    val person: SearchablePerson = create.searchable[SearchablePerson]
    val lastNameKeywords: KeywordIndexing = KeywordIndexing(
      lucene = this,
      directoryName = "lastNames",
      wordsFromBuilder = KeywordIndexing.FieldFromBuilder(person.lastName)
    )
  }

  trait SearchablePerson extends Searchable[Person] {
    // We must implement the criteria for updating and deleting
    override def idSearchTerms(t: Person): List[SearchTerm] = List(exact(id(t.id)))

    implicit def stringifyId[T]: Stringify[Id[T]] = Stringify[Id[T]]((s: String) => Id[T](s))

    // We can create custom / explicit configurations for each field
    val id: Field[Id[Person]] = lucene.create.stringifiedField[Id[Person]]("id", fullTextSearchable = false)

    // Alternatively, we can simply define them like an interface and they will be defined automatically at compile-time
    def firstName: Field[String]
    def lastName: Field[String]

    val tags: FacetField = lucene.create.facet("tags", multiValued = true)
  }

  case class Person(id: Id[Person], firstName: String, lastName: String, age: Int, address: String, city: String, state: String, zip: String, aliases: List[String])

  case class Id[T](value: String) {
    override def toString: String = value
  }
}