package com.outr.lucene4s.query

import java.io.StringReader

import com.outr.lucene4s.{Length, Lucene}
import com.outr.lucene4s.facet.FacetField
import com.outr.lucene4s.field.Field
import com.outr.lucene4s.field.value.SpatialPoint
import org.apache.lucene.document._
import org.apache.lucene.facet.DrillDownQuery
import org.apache.lucene.geo.Polygon
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search._
import org.apache.lucene.util.automaton.RegExp

sealed trait SearchTerm {
  protected[lucene4s] def toLucene(lucene: Lucene): Query
}

object MatchAllSearchTerm extends SearchTerm {
  private lazy val instance = new MatchAllDocsQuery

  override protected[lucene4s] def toLucene(lucene: Lucene): Query = instance

  override def toString: String = "matchAll()"
}

class ParsableSearchTerm(field: Option[Field[String]], value: String, allowLeadingWildcard: Boolean) extends SearchTerm {
  override def toLucene(lucene: Lucene): Query = {
    val parser = new QueryParser(field.getOrElse(lucene.fullText).filterName, lucene.analyzer)
    parser.setAllowLeadingWildcard(allowLeadingWildcard)
    parser.parse(value)
  }

  override def toString: String = s"parse($field, value: $value, allowLeadingWildcard: $allowLeadingWildcard)"
}

class PhraseSearchTerm(field: Option[Field[String]], value: String, slop: Int = 0) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = {
    new PhraseQuery(slop, field.getOrElse(lucene.fullText).filterName, value.split(' ').map(_.toLowerCase): _*)
  }

  override def toString: String = s"phrase($field, value: $value, slop: $slop)"
}

class TermSearchTerm(field: Option[Field[String]], value: String) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = new TermQuery(new Term(field.getOrElse(lucene.fullText).filterName, value))

  override def toString: String = s"term(${field.map(_.filterName)} = $value)"
}

class DrillDownSearchTerm(facet: FacetField, path: Seq[String], onlyThisLevel: Boolean) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = {
    val indexedField = lucene.facetsConfig.getDimConfig(facet.name).indexFieldName
    val exactPath = if (onlyThisLevel) {
      path.toList ::: List("$ROOT$")
    } else {
      path
    }
    new TermQuery(DrillDownQuery.term(indexedField, facet.name, exactPath: _*))
  }

  override def toString: String = s"drillDown(facet: $facet, path: $path)"
}

class MoreLikeThisSearchTerm(field: Option[Field[String]], value: String,
                             minTermFreq: Int,
                             minDocFreq: Int,
                             maxDocFreq: Int,
                             boost: Boolean,
                             minWordLen: Int,
                             maxWordLen: Int,
                             maxQueryTerms: Int) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = {
    val fieldName = field.getOrElse(lucene.fullText).filterName

    val mlt = lucene.moreLikeThis
    mlt.setFieldNames(Array[String](fieldName))
    mlt.setMinTermFreq(minTermFreq)
    mlt.setMinDocFreq(minDocFreq)
    mlt.setMaxDocFreq(maxDocFreq)
    mlt.setBoost(boost)
    mlt.setMinWordLen(minWordLen)
    mlt.setMaxWordLen(maxWordLen)
    mlt.setMaxQueryTerms(maxQueryTerms)

    val query = mlt.like(fieldName, new StringReader(value))
    query
  }

  override def toString: String = s"mlt(${field.map(_.storeName)} = $value)"
}

class ExactBooleanSearchTerm(field: Field[Boolean], value: Boolean) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = IntPoint.newExactQuery(field.filterName, if (value) 1 else 0)

  override def toString: String = s"term(${field.storeName} = $value)"
}

class ExactIntSearchTerm(field: Field[Int], value: Int) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = IntPoint.newExactQuery(field.filterName, value)

  override def toString: String = s"term(${field.storeName} = $value)"
}

class ExactLongSearchTerm(field: Field[Long], value: Long) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = LongPoint.newExactQuery(field.filterName, value)

  override def toString: String = s"term(${field.storeName} = $value)"
}

class ExactDoubleSearchTerm(field: Field[Double], value: Double) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = DoublePoint.newExactQuery(field.filterName, value)

  override def toString: String = s"term(${field.storeName} = $value)"
}

class RangeIntSearchTerm(field: Field[Int], start: Int, end: Int) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = IntPoint.newRangeQuery(field.filterName, start, end)

  override def toString: String = s"range(${field.storeName}, start: $start, end: $end)"
}

class RangeLongSearchTerm(field: Field[Long], start: Long, end: Long) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = LongPoint.newRangeQuery(field.filterName, start, end)

  override def toString: String = s"range(${field.storeName}, start: $start, end: $end)"
}

class RangeDoubleSearchTerm(field: Field[Double], start: Double, end: Double) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = DoublePoint.newRangeQuery(field.filterName, start, end)

  override def toString: String = s"range(${field.storeName}, start: $start, end: $end)"
}

class SetIntSearchTerm(field: Field[Int], set: Seq[Int]) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = IntPoint.newSetQuery(field.filterName, set: _*)

  override def toString: String = s"set(${field.storeName}, set: $set)"
}

class SetLongSearchTerm(field: Field[Long], set: Seq[Long]) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = LongPoint.newSetQuery(field.filterName, set: _*)

  override def toString: String = s"set(${field.storeName}, set: $set)"
}

class SetDoubleSearchTerm(field: Field[Double], set: Seq[Double]) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = DoublePoint.newSetQuery(field.filterName, set: _*)

  override def toString: String = s"set(${field.storeName}, set: $set)"
}

class RegexpSearchTerm(field: Option[Field[String]], value: String) extends SearchTerm {
  // TODO: add support for regular expression flags
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = new RegexpQuery(new Term(field.getOrElse(lucene.fullText).filterName, value), RegExp.ALL)

  override def toString: String = s"regexp(${field.map(_.storeName)}, value: $value)"
}

class WildcardSearchTerm(field: Option[Field[String]], value: String) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = new WildcardQuery(new Term(field.getOrElse(lucene.fullText).filterName, value))

  override def toString: String = s"wildcard(${field.map(_.storeName)}, value: $value)"
}

class FuzzySearchTerm(field: Option[Field[String]], value: String) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = new FuzzyQuery(new Term(field.getOrElse(lucene.fullText).filterName, value))

  override def toString: String = s"fuzzy(${field.map(_.storeName)}, value: $value)"
}

class SpatialBoxTerm(field: Field[SpatialPoint], minLatitude: Double, maxLatitude: Double, minLongitude: Double, maxLongitude: Double) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = LatLonPoint.newBoxQuery(field.filterName, minLatitude, maxLatitude, minLongitude, maxLongitude)

  override def toString: String = s"spatialBox(${field.storeName}, minLatitude: $minLatitude, maxLatitude: $maxLatitude, minLongitude: $minLongitude, maxLongitude: $maxLongitude)"
}

class SpatialDistanceTerm(field: Field[SpatialPoint], point: SpatialPoint, radius: Length) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = LatLonPoint.newDistanceQuery(field.filterName, point.latitude, point.longitude, radius.meters)

  override def toString: String = s"spatialDistance(${field.storeName}, latitude: ${point.latitude}, longitude: ${point.longitude}, radius: $radius)"
}

class SpatialPolygonTerm(field: Field[SpatialPoint], polygons: List[SpatialPolygon]) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = LatLonPoint.newPolygonQuery(field.filterName, polygons.map(_.toLucene): _*)

  override def toString: String = s"spatialPolygon(${field.storeName}, polygons: ${polygons.mkString("[", ", ", "]")})"
}

case class SpatialPolygon(points: List[SpatialPoint], holes: List[SpatialPolygon] = Nil) {
  private[lucene4s] def toLucene: Polygon = {
    val lats = points.map(_.latitude).toArray
    val lons = points.map(_.longitude).toArray
    new Polygon(lats, lons, holes.map(_.toLucene): _*)
  }
}

case class GroupedSearchTerm(minimumNumberShouldMatch: Int,
                             conditionalTerms: List[(SearchTerm, Condition)]) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = {
    val b = new BooleanQuery.Builder
    b.setMinimumNumberShouldMatch(minimumNumberShouldMatch)
    if (conditionalTerms.forall(_._2 == Condition.MustNot)) {           // Work-around for all negative groups, something must match
      b.add(MatchAllSearchTerm.toLucene(lucene), Condition.Must.occur)
    }
    conditionalTerms.foreach {
      case (st, c) => b.add(st.toLucene(lucene), c.occur)
    }
    b.build()
  }

  override def toString: String = s"grouped(minimumNumberShouldMatch: $minimumNumberShouldMatch, conditionalTerms: ${conditionalTerms.map(ct => s"${ct._1} -> ${ct._2}").mkString(", ")})"
}

case class BoostedSearchTerm(term: SearchTerm, boost: Double) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = {
    new BoostQuery(term.toLucene(lucene), boost.toFloat)
  }

  override def toString: String = s"boosted(term: $term, boost: $boost)"
}