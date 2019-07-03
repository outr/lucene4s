# lucene4s

[![Build Status](https://travis-ci.org/outr/lucene4s.svg?branch=master)](https://travis-ci.org/outr/lucene4s)
[![Stories in Ready](https://badge.waffle.io/outr/lucene4s.png?label=ready&title=Ready)](https://waffle.io/outr/lucene4s)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/outr/lucene4s)
[![Maven Central](https://img.shields.io/maven-central/v/com.outr/lucene4s_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/com.outr/lucene4s_2.12)

Light-weight convenience wrapper around Lucene to simplify complex tasks and add Scala sugar.

## Setup

lucene4s is published to Sonatype OSS and Maven Central currently supporting Scala 2.11, 2.12, and 2.13.

Configuring the dependency in SBT simply requires:

```
libraryDependencies += "com.outr" %% "lucene4s" % "1.8.1"
```

## Using

### Imports

You may find yourself needing other imports depending on what you're doing, but the majority of functionality can be
achieved simply importing `com.outr.lucene4s._`:

```scala
import com.outr.lucene4s._
```

### Creating a Lucene Instance

`Lucene` is the object utilized for doing anything with Lucene, so you first need to instantiate it:

```scala
val directory = Paths.get("index")
val lucene = new DirectLucene(Nil, directory = Option(directory))
```

NOTE: If you leave `directory` blank or set it to None (the default) it will use an in-memory index. 

### Creating Fields

For type-safety and convenience we can create the fields we'll be using in the document ahead of time:

```scala
val name = lucene.create.field[String]("name")
val address = lucene.create.field[String]("address")
```

### Inserting Documents

Inserting is quite easy using the document builder:

```scala
lucene.doc().fields(name("John Doe"), address("123 Somewhere Rd.")).index()
lucene.doc().fields(name("Jane Doe"), address("123 Somewhere Rd.")).index()
```

### Querying Documents

Querying documents is just as easy with the query builder:

```scala
val paged = lucene.query().sort(Sort(name)).search()
paged.results.foreach { searchResult =>
  println(s"Name: ${searchResult(name)}, Address: ${searchResult(address)}")
}
```

This will return a `PagedResults` instance with the page size set to the `limit`. There are convenience methods for
navigating the pagination and accessing the results.

The above code will output:

```
Name: John Doe, Address: 123 Somewhere Rd.
Name: Jane Doe, Address: 123 Somewhere Rd.
```

### Highlighting Results

Though querying is nice, we may want to stylize the output to show the matched results. This is pretty simple:

```scala
val paged = lucene.query().sort(Sort(name)).filter(fuzzy(name("jhn"))).highlight().search()
paged.results.foreach { searchResult =>
  val highlighting = searchResult.highlighting(name).head
  println(s"Fragment: ${highlighting.fragment}, Word: ${highlighting.word}")
}
```

The above code will output:

```
Fragment: <em>John</em> Doe, Word: John
Fragment: <em>Jane</em> Doe, Word: Jane
```

### Faceted Searching

See https://github.com/outr/lucene4s/blob/master/implementation/src/test/scala/tests/FacetsSpec.scala

### Full-Text Searching

In lucene4s the `Lucene` instance holds a `fullText` `Field` that contains a concatenation of all the fields that
are configured as `fullTextSearchable`.  This defaults to `Lucene.defaultFullTextSearchable` which defaults to false.

The `fullText` field is the default field used for searches if it's not specified in the `SearchTerm`. Let's see an example:

```
val paged = lucene.query().filter(wildcard("doe*")).search()
paged.total should be(4)
paged.results(0)(firstName) should be("John")
paged.results(1)(firstName) should be("Jane")
paged.results(2)(firstName) should be("Baby")
paged.results(3)(firstName) should be("James")
```

For a complete example, see: https://github.com/outr/lucene4s/blob/master/implementation/src/test/scala/tests/FullTextSpec.scala

### Keyword Searching

As we saw previously, the `fullText` field provides us with a concatenation of all fields configured to be `fullTextSearchable`.
In addition, if you create an instance of `KeywordIndexing` you can query against a no-duplicates index of keywords for
the `fullText` (although you can override defaults to apply keyword indexing to any field). All we have to do is create
and instance referencing the `Lucene` instance and the name (used for storage purposes):

```
val keywordIndexing = KeywordIndexing(lucene, "keywords")
val keywords = keywordIndexing.search("do*")
println("Keywords: ${keywords.results.map(_.word).mkString(", ")}")
```

The above code would output:

```
Keywords: Doe
```

For the complete example see: https://github.com/outr/lucene4s/blob/master/implementation/src/test/scala/tests/SimpleSpec.scala

### Case Class Support

lucene4s provides a powerful Macro-based system to generate two-way mappings between case classes and Lucene fields at
compile-time. This is accomplished through the use of `Searchable`.  The setup is pretty simple.

#### Setup

First we need to define a case class to model the data in the index:

```scala
case class Person(id: Int, firstName: String, lastName: String, age: Int, address: String, city: String, state: String, zip: String)
```

As you can see, this is a bare-bones case class with nothing special about it.

Next we need to define a `Searchable` trait the defines the unique identification for update and delete:

```scala
trait SearchablePerson extends Searchable[Person] {
  // This is necessary for update and delete to reference the correct document.
  override def idSearchTerms(person: Person): List[SearchTerm] = List(exact(id(person.id)))
  
  /*
    Though at compile-time all fields will be generated from the params in `Person`, for code-completion we can define
    an unimplemented method in order to properly reference the field. This will still compile without this definition,
    but most IDEs will complain.
   */
  def id: Field[Int]
}
```

As the last part of our set up we simply need to generate it from our `Lucene` instance:

```scala
val people = lucene.create.searchable[SearchablePerson]
```

#### Inserting

Now that we've configured everything inserting a person is trivial:

```scala
people.insert(Person(1, "John", "Doe", 23, "123 Somewhere Rd.", "Lalaland", "California", "12345")).index()
```

Notice that we still have to call `index()` at the end for it to actually invoke. This allows us to do more advanced
tasks like adding facets, adding non-Searchable fields, etc. before actually inserting.

#### Updating

Now lets try updating our `Person`:

```scala
people.update(Person(1, "John", "Doe", 23, "321 Nowhere St.", "Lalaland", "California", "12345")).index()
```

As you can see here, the signature is quite similar to `insert`. Internally this will utilize `idSearchTerms` as we
declared previously to apply the update. In this case that means as long as we don't change the id (1) then calls to
update will replace an existing record if one exists.

#### Querying

Querying works very much the same as in the previous examples, except we get our `QueryBuilder` from our `people`
instance:

```scala
val paged = people.query().search()
paged.entries.foreach { person =>
  println(s"Person: $person")
}
```

Note that instead of calling `paged.results` we call `paged.entries` as it represents the conversion to `Person`. We can
still use `paged.results` if we want access to the `SearchResult` like before.

#### Deleting

Deleting is just as easy as inserting and updating:

```scala
people.delete(Person(1, "John", "Doe", 23, "321 Nowhere St.", "Lalaland", "California", "12345"))
```

#### Additional Information

All `Searchable` implementations automatically define a `docType` field that is used to uniquely separate different
`Searchable` instances so you don't have to worry about multiple different instances overlapping.

For more examples see https://github.com/outr/lucene4s/blob/master/implementation/src/test/scala/tests/SearchableSpec.scala

### Geospatial Support

One of the great features of Lucene is geospatial querying and what Lucene wrapper would be complete without it?

#### Creating a Spatial Field

In order to create a stored, queryable, filterable, and sortable latitude and longitude you need only create a
`SpatialPoint` field:

```scala
val location: Field[SpatialPoint] = lucene.create.field[SpatialPoint]("location")
```

#### Sorting Nearest a Point

Most of the time it's most useful to take an existing latitude and longitude and sort your results returning the
nearest documents to that location:

```scala
val paged = lucene.query().sort(Sort.nearest(location, SpatialPoint(40.7142, -74.0119))).search()
```

#### Filtering by Distance

If you want to filter your results to only include entries within a certain range of a location:

```scala
import squants.space.LengthConversions._

val newYorkCity = SpatialPoint(40.7142, -74.0119)
val paged = lucene
  .query()
  .sort(Sort.nearest(location, newYorkCity))
  .filter(spatialDistance(location, newYorkCity, 50.miles))
  .search()
```

Note the import from `squants`.  We use Squants (https://github.com/typelevel/squants) for distance representations so
that you can use `miles`, `meters`, or whatever measurement of distance desirable.

## Versions

### Features for 1.9.0 (In-Progress)

* [ ] Scala.js support for Query parsing and URL mapping (QueryURL)
* [ ] Complete ScalaDocing

### Features for 1.8.0 (Released 2018.05.09)

* [X] Support to update documents returned from a search
* [X] Access to fields and facets associated with a `Lucene` instance

### Features for 1.7.0 (Released 2018.05.07)

* [X] In-Place Replaceable Indexes
* [X] Numeric operator querying (<, >, <=, >=, etc.)
* [X] Numeric Seq support

### Features for 1.6.0 (Released 2018.04.05)

* [X] Re-working of facets in queries to be more in-line with standard queries

### Features for 1.5.0 (Released 2017.08.11)

* [X] Add better facet searching drilldown to support only that level or that level and lower

### Features for 1.4.0 (Released 2016.12.17)

* [X] Geospatial features

### Features for 1.3.0 (Released 2016.12.06)

* [X] Better Highlighting support
* [X] Add Scala 2.12 support
* [X] Keyword indexing / autocomplete support
* [X] Add support for FacetQuery Conditions (MustNot and Should support)
* [X] Range querying

### Features for 1.2.0 (Released 2016.11.23)

* [X] Highlighting support
* [X] Code Cleanup and Optimizations

### Features for 1.1.0 (Released 2016.10.17)

* [X] Better field integrations and convenience implicits
* [X] Support for storing and retrieving case classes as documents via compile-time Macro
* [X] Numeric storage and retrieval functionality (Boolean, Int, Long, and Double)
* [X] Facets support in Searchable
* [X] Support for docType on Searchable to provide multiple Searchable implementation in a single index

### Features for 1.0.0 (Released 2016.10.13)

* [X] Simplified set up of readers, writers, facets, etc. all under `Lucene`
* [X] DSL for inserting documents in a type-safe way
* [X] DSL for querying documents in a type-safe way
* [X] Facet searching support
* [X] Full-Text search functionality
* [X] Query Builder to simplify querying against Lucene
* [X] Solid test coverage
* [X] Updating and Deleting documents
* [X] Structured query terms builder
