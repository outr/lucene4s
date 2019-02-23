package tests

import com.outr.lucene4s._
import com.outr.lucene4s.facet.{FacetField, FacetValue}
import com.outr.lucene4s.field.Field
import com.outr.lucene4s.query.{Condition, SearchTerm}
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

      val sergeBrin = Person("Serge Brin", "sb2@google.com", 43, List(austin))
      val drJohnHennessy = Person("Dr. John L. Hennessy, Ph.D.", "djh@google.com", 65, List(birmingham))
    }
  }

  "Complex Query Spec" should {
    def names(names: String*): SearchTerm = grouped(
      minimumNumberShouldMatch = 1,
      names.map(n => term(s.name(n)) -> Condition.Should): _*
    )

    def s = PersonSearch
    "insert some documents" in {
      s.insert(values.people.johnHennessy)
      s.insert(values.people.sergeyBrin)
      s.insert(values.people.lawrencePage)
      s.insert(values.people.ruthPorat)
      s.insert(values.people.sundarPichai)
      s.insert(values.people.davidDrummond)

      s.insert(values.people.sergeBrin)
      s.insert(values.people.drJohnHennessy)
    }
    "query to retrieve John Hennessy by name" in {
      val results = s.query().scoreDocs().filter(grouped(
        minimumNumberShouldMatch = 2,
        term(s.name("John")) -> Condition.Should,
        term(s.name("Hennessy")) -> Condition.Should
      )).search()
      results.total should be(2)
      val result = results.results.head
      result(s.name) should be("John Hennessy")
      result.score should be > 2.5
      results.results.last.score should be < 2.0
    }
    "query to retrieve Dr. John Hennessy by name" in {
      val results = s.query().scoreDocs().filter(grouped(
        minimumNumberShouldMatch = 2,
        term(s.name("Dr")) -> Condition.Should,
        term(s.name("John")) -> Condition.Should,
        term(s.name("Hennessy")) -> Condition.Should,
        term(s.name("PhD")) -> Condition.Should
      )).search()
      results.total should be(2)
      val result = results.results.head
      result(s.name) should be("Dr. John L. Hennessy, Ph.D.")
    }
    "query to retrieve Sergey Brin by name" in {
      val results = s.query().scoreDocs().filter(grouped(
        minimumNumberShouldMatch = 2,
        term(s.name("Sergey")) -> Condition.Should,
        term(s.name("Brin")) -> Condition.Should
      )).search()
      results.total should be(1)
      val result = results.results.head
      result(s.name) should be("Sergey Brin")
    }
    "query to retrieve Sergey Brin by name with middle" in {
      val results = s.query().scoreDocs().filter(grouped(
        minimumNumberShouldMatch = 2,
        term(s.name("Sergey")) -> Condition.Should,
        term(s.name("John")) -> Condition.Should,
        term(s.name("Brin")) -> Condition.Should
      )).search()
      results.total should be(1)
      val result = results.results.head
      result(s.name) should be("Sergey Brin")
    }
    "query to retrieve Sergey Brin by name with nicknames" in {
      val results = s.query().scoreDocs().filter(grouped(
        minimumNumberShouldMatch = 2,
        names("Gey", "Serge", "Sergey") -> Condition.Should,
        term(s.name("Brin")) -> Condition.Should
      )).search()
      results.total should be(2)
      val result = results.results.head
      result(s.name) should be("Sergey Brin")
    }
    "query to retrieve Sergey Brin by name with address" in {
      val nameGroup = grouped(
        minimumNumberShouldMatch = 2,
        names("Gey", "Serge", "Sergey") -> Condition.Should,
        term(s.name("Brin")) -> Condition.Should
      )
      val address = grouped(
        minimumNumberShouldMatch = 1,
        drillDown(s.city("Denver")) -> Condition.Should,
        drillDown(s.state("CO")) -> Condition.Should
      )
      val filter = grouped(
        minimumNumberShouldMatch = 0,
        nameGroup -> Condition.Must,
        address -> Condition.Should
      )
      val results = s.query().scoreDocs().filter(filter).search()
      results.total should be(2)
      val result = results.results.head
      result(s.name) should be("Sergey Brin")
    }
    "query to retrieve Ruth Porat by partial name and email address" in {
      val nameGroup = grouped(
        minimumNumberShouldMatch = 1,
        names("Ruthy", "R") -> Condition.Should,
        boost(names("Porat"), 2.0) -> Condition.Should
      )
      val filter = grouped(
        minimumNumberShouldMatch = 1,
        nameGroup -> Condition.Should,
        boost(drillDown(s.email("rp@google.com")), 5.0) -> Condition.Should
      )
      val results = s.query().scoreDocs().filter(filter).search()
      results.total should be(1)
      val result = results.results.head
      result(s.name) should be("Ruth Porat")
      result.score shouldBe 13.0 +- 0.3
    }
  }

  object PersonSearch extends DirectLucene(uniqueFields = List("name"), defaultFullTextSearchable = true, autoCommit = true) {
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
