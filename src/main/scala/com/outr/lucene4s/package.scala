package com.outr

import com.outr.lucene4s.field.Field
import com.outr.lucene4s.field.value.{FieldAndValue, SpatialPoint}
import com.outr.lucene4s.field.value.support._
import com.outr.lucene4s.query._
import org.apache.lucene.queries.mlt.MoreLikeThis

import scala.language.implicitConversions

package object lucene4s {
  implicit def stringSupport: ValueSupport[String] = StringValueSupport
  implicit def booleanSupport: ValueSupport[Boolean] = BooleanValueSupport
  implicit def intSupport: ValueSupport[Int] = IntValueSupport
  implicit def longSupport: ValueSupport[Long] = LongValueSupport
  implicit def doubleSupport: ValueSupport[Double] = DoubleValueSupport
  implicit def spatialPointSupport: ValueSupport[SpatialPoint] = SpatialPointValueSupport

  implicit def string2ParsableSearchTerm(value: String): SearchTerm = parse(value)

  implicit val booleanFieldValue2SearchTerm: (FieldAndValue[Boolean]) => SearchTerm = {
    (fv: FieldAndValue[Boolean]) => new ExactBooleanSearchTerm(fv.field, fv.value)
  }
  implicit val intFieldValue2SearchTerm: (FieldAndValue[Int]) => SearchTerm = {
    (fv: FieldAndValue[Int]) => new ExactIntSearchTerm(fv.field, fv.value)
  }
  implicit val longFieldValue2SearchTerm: (FieldAndValue[Long]) => SearchTerm = {
    (fv: FieldAndValue[Long]) => new ExactLongSearchTerm(fv.field, fv.value)
  }
  implicit val doubleFieldValue2SearchTerm: (FieldAndValue[Double]) => SearchTerm = {
    (fv: FieldAndValue[Double]) => new ExactDoubleSearchTerm(fv.field, fv.value)
  }
  implicit val stringFieldValue2SearchTerm: (FieldAndValue[String]) => SearchTerm = {
    (fv: FieldAndValue[String]) => new PhraseSearchTerm(Some(fv.field), fv.value)
  }

  def matchAll(): SearchTerm = MatchAllSearchTerm

  def parse(field: Field[String], value: String): ParsableSearchTerm = parse(field, value, allowLeadingWildcard = false)
  def parse(field: Field[String], value: String, allowLeadingWildcard: Boolean): ParsableSearchTerm = new ParsableSearchTerm(Some(field), value, allowLeadingWildcard)
  def parse(value: String): ParsableSearchTerm = parse(value, allowLeadingWildcard = false)
  def parse(value: String, allowLeadingWildcard: Boolean): ParsableSearchTerm = new ParsableSearchTerm(None, value, allowLeadingWildcard)

  def parseFuzzy(text: String, field: Option[Field[String]] = None): ParsableSearchTerm = {
    val queryText = text.filterNot(Lucene.specialCharacters.contains).split(' ').flatMap {
      case word if word.trim.isEmpty => None
      case word => Some(s"$word~")
    }.mkString("(", " AND ", ")")
    new ParsableSearchTerm(field, queryText, allowLeadingWildcard = false)
  }

  def parseQuery(text: String,
                 field: Option[Field[String]] = None,
                 allowLeadingWildcard: Boolean = false,
                 includeFuzzy: Boolean = false): ParsableSearchTerm = {
    val queryText = text.split(' ').map {
      case word if Lucene.isLuceneWord(word) => word
      case word if !allowLeadingWildcard && !includeFuzzy => s"$word*"
      case word => {
        val b = new StringBuilder("(")
        // Starts with, boosted
        b.append(word)
        b.append("*^4")

        if (allowLeadingWildcard) {
          // Leading wildcard
          b.append(" OR *")
          b.append(word)
          b.append("*")
        }
        if (includeFuzzy) {
          // Fuzzy matches
          b.append(" OR ")
          b.append(word)
          b.append("~")
        }

        b.append(")")
        b.toString()
      }
    }.mkString("(", " ", ")")
    new ParsableSearchTerm(field, queryText, allowLeadingWildcard)
  }

  def term(fv: FieldAndValue[String]): TermSearchTerm = new TermSearchTerm(Some(fv.field), fv.value.toString.toLowerCase)
  def term(value: String): TermSearchTerm = new TermSearchTerm(None, value)

  def exact[T](fv: FieldAndValue[T])(implicit fv2SearchTerm: FieldAndValue[T] => SearchTerm): SearchTerm = fv2SearchTerm(fv)
  def intRange(field: Field[Int], start: Int, end: Int): SearchTerm = new RangeIntSearchTerm(field, start, end)
  def longRange(field: Field[Long], start: Long, end: Long): SearchTerm = new RangeLongSearchTerm(field, start, end)
  def doubleRange(field: Field[Double], start: Double, end: Double): SearchTerm = new RangeDoubleSearchTerm(field, start, end)

  def regexp(fv: FieldAndValue[String]): RegexpSearchTerm = new RegexpSearchTerm(Some(fv.field), fv.value.toString)
  def regexp(value: String): RegexpSearchTerm = new RegexpSearchTerm(None, value)

  def wildcard(fv: FieldAndValue[String]): WildcardSearchTerm = new WildcardSearchTerm(Some(fv.field), fv.value.toString)
  def wildcard(value: String): WildcardSearchTerm = new WildcardSearchTerm(None, value)

  def fuzzy(value: String): FuzzySearchTerm = new FuzzySearchTerm(None, value)
  def fuzzy(fv: FieldAndValue[String]): FuzzySearchTerm = new FuzzySearchTerm(Some(fv.field), fv.value.toString)

  def mltFullText(value: String,
          minTermFreq: Int = MoreLikeThis.DEFAULT_MIN_TERM_FREQ,
          minDocFreq: Int = MoreLikeThis.DEFAULT_MIN_DOC_FREQ,
          maxDocFreq: Int = MoreLikeThis.DEFAULT_MAX_DOC_FREQ,
          boost: Boolean = MoreLikeThis.DEFAULT_BOOST,
          minWordLen: Int = MoreLikeThis.DEFAULT_MIN_WORD_LENGTH,
          maxWordLen: Int = MoreLikeThis.DEFAULT_MAX_WORD_LENGTH,
          maxQueryTerms: Int = MoreLikeThis.DEFAULT_MAX_QUERY_TERMS): MoreLikeThisSearchTerm =
    new MoreLikeThisSearchTerm(None, value,
      minTermFreq = minTermFreq,
      minDocFreq = minDocFreq,
      maxDocFreq = maxDocFreq,
      boost = boost,
      minWordLen = minWordLen,
      maxWordLen = maxWordLen,
      maxQueryTerms = maxQueryTerms)


  def mlt(fv: FieldAndValue[String],
          minTermFreq: Int = MoreLikeThis.DEFAULT_MIN_TERM_FREQ,
          minDocFreq: Int = MoreLikeThis.DEFAULT_MIN_DOC_FREQ,
          maxDocFreq: Int = MoreLikeThis.DEFAULT_MAX_DOC_FREQ,
          boost: Boolean = MoreLikeThis.DEFAULT_BOOST,
          minWordLen: Int = MoreLikeThis.DEFAULT_MIN_WORD_LENGTH,
          maxWordLen: Int = MoreLikeThis.DEFAULT_MAX_WORD_LENGTH,
          maxQueryTerms: Int = MoreLikeThis.DEFAULT_MAX_QUERY_TERMS): MoreLikeThisSearchTerm =
    new MoreLikeThisSearchTerm(Some(fv.field), fv.value.toString,
      minTermFreq = minTermFreq,
      minDocFreq = minDocFreq,
      maxDocFreq = maxDocFreq,
      boost = boost,
      minWordLen = minWordLen,
      maxWordLen = maxWordLen,
      maxQueryTerms = maxQueryTerms)

  def spatialBox(field: Field[SpatialPoint], minLatitude: Double, maxLatitude: Double, minLongitude: Double, maxLongitude: Double): SpatialBoxTerm = new SpatialBoxTerm(field, minLatitude, maxLatitude, minLongitude, maxLongitude)

  def spatialDistance(field: Field[SpatialPoint], point: SpatialPoint, radiusInMeters: Long): SpatialDistanceTerm = new SpatialDistanceTerm(field, point, radiusInMeters)

  def spatialPolygon(field: Field[SpatialPoint], polygons: SpatialPolygon*): SpatialPolygonTerm = new SpatialPolygonTerm(field, polygons.toList)

  def grouped(minimumNumberShouldMatch: Int,
              entries: (SearchTerm, Condition)*): GroupedSearchTerm = new GroupedSearchTerm(
    minimumNumberShouldMatch = minimumNumberShouldMatch,
    conditionalTerms = entries.toList
  )
  def grouped(entries: (SearchTerm, Condition)*): GroupedSearchTerm = grouped(0, entries: _*)
}