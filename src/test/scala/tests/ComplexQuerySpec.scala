package tests

import com.outr.lucene4s._
import com.outr.lucene4s.facet.{FacetField, FacetValue}
import com.outr.lucene4s.field.Field
import com.outr.lucene4s.query.Condition
import org.scalatest.{Matchers, WordSpec}

class ComplexQuerySpec extends WordSpec with Matchers {
  object values {
    object addresses {
      val annArbor = Address("2300 Traverwood Dr", "", "Ann Arbor", "MI", "48105")
      val atlanta = Address("10 10th Street NE", "", "Atlanta", "GA", "30309")
      val austin = Address("500 W 2nd St", "Suite 2900", "Austin", "TX", "78701")
      val birmingham = Address("114 Willits Street", "", "Birmingham", "MI", "48009")
      val boulder = Address("2600 Pearl Street", "", "Boulder", "CO", "80302")
    }
    object people {
      import values.addresses._

      val johnHennessy = Person("John Hennessy", "jh@google.com", 65, List(annArbor, atlanta))
      val sergeyBrin = Person("Sergey Brin", "sb@google.com", 43, List(boulder))
      val lawrencePage = Person("Lawrence Page", "lp@google.com", 44, List(austin))
      val ruthPorat = Person("Ruth Porat", "rp@google.com", 59, List(birmingham))
      val sundarPichai = Person("Sundar Pichai", "sp@google.com", 46, List(boulder, annArbor))
      val davidDrummond = Person("David Drummond", "dd@google.com", 54, Nil)
    }
  }

  "Complex Query Spec" should {
    def s = PersonSearch
    "insert some documents" in {
      s.insert(values.people.johnHennessy)
      s.insert(values.people.sergeyBrin)
      s.insert(values.people.lawrencePage)
      s.insert(values.people.ruthPorat)
      s.insert(values.people.sundarPichai)
      s.insert(values.people.davidDrummond)
    }
    "simple query to retrieve John Hennessy by name" in {
      val results = s.query().filter(grouped(
        minimumNumberShouldMatch = 2,
        term(s.name("John")) -> Condition.Should,
        term(s.name("Hennessy")) -> Condition.Should
      )).search()
      results.total should be(1L)
      val result = results.results.head
      result(s.name) should be("John Hennessy")
    }
    "simple query to retrieve Sergey Brin by name" in {
      val results = s.query().filter(grouped(
        minimumNumberShouldMatch = 2,
        term(s.name("Sergey")) -> Condition.Should,
        term(s.name("Brin")) -> Condition.Should
      )).search()
      results.total should be(1L)
      val result = results.results.head
      result(s.name) should be("Sergey Brin")
    }
    "simple query to retrieve Sergey Brin by name with middle" in {
      val results = s.query().filter(grouped(
        minimumNumberShouldMatch = 2,
        term(s.name("Sergey")) -> Condition.Should,
        term(s.name("John")) -> Condition.Should,
        term(s.name("Brin")) -> Condition.Should
      )).search()
      results.total should be(1L)
      val result = results.results.head
      result(s.name) should be("Sergey Brin")
    }
    "simple query to retrieve Sergey Brin by name with nicknames" in {
      val results = s.query().filter(grouped(
        minimumNumberShouldMatch = 2,
        grouped(
          minimumNumberShouldMatch = 1,
          term(s.name("Gey")) -> Condition.Should,
          term(s.name("Serge")) -> Condition.Should,
          term(s.name("Sergey")) -> Condition.Should
        ) -> Condition.Should,
        term(s.name("Brin")) -> Condition.Should
      )).search()
      results.total should be(1L)
      val result = results.results.head
      result(s.name) should be("Sergey Brin")
    }
    // TODO: include city and state and have Serge Brin in another address
  }

  object PersonSearch extends Lucene(defaultFullTextSearchable = true, autoCommit = true) {
    val name: Field[String] = create.field[String]("name")
    val email: FacetField = create.facet("email", multiValued = true)
    val age: Field[Int] = create.field[Int]("age")
    val line1: FacetField = create.facet("line1", multiValued = true)
    val line2: FacetField = create.facet("line2", multiValued = true)
    val city: FacetField = create.facet("city", multiValued = true)
    val state: FacetField = create.facet("state", multiValued = true)
    val zip: FacetField = create.facet("zip", multiValued = true)

    def insert(person: Person): Unit = {
      def s2Option(s: String, f: FacetField): Option[FacetValue] = s match {
        case null | "" => None
        case _ => Some(f(s))
      }
      val addressFields = person.addresses.flatMap { address =>
        List(
          s2Option(address.line1, line1),
          s2Option(address.line2, line2),
          s2Option(address.city, city),
          s2Option(address.state, state),
          s2Option(address.zip, zip)
        ).flatten
      }
      doc().fields(
        name(person.name),
        age(person.age)
      ).facets(
        email(person.email)
      ).facets(addressFields: _*).index()
      commit()
    }
  }

  case class Person(name: String, email: String, age: Int, addresses: List[Address])

  case class Address(line1: String, line2: String, city: String, state: String, zip: String)
}
