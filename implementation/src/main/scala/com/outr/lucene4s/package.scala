package com.outr

import com.outr.lucene4s.facet.FacetValue
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

  implicit val listStringSupport: ValueSupport[List[String]] = new ListValueSupport[String](stringSupport)
  implicit val listBooleanSupport: ValueSupport[List[Boolean]] = new ListValueSupport[Boolean](booleanSupport)
  implicit val listIntSupport: ValueSupport[List[Int]] = new ListValueSupport[Int](intSupport)
  implicit val listLongSupport: ValueSupport[List[Long]] = new ListValueSupport[Long](longSupport)
  implicit val listDoubleSupport: ValueSupport[List[Double]] = new ListValueSupport[Double](doubleSupport)
  implicit val listSpatialSupport: ValueSupport[List[SpatialPoint]] = new ListValueSupport[SpatialPoint](spatialPointSupport)

  implicit val optionStringSupport: ValueSupport[Option[String]] = new OptionValueSupport[String](stringSupport)
  implicit val optionBooleanSupport: ValueSupport[Option[Boolean]] = new OptionValueSupport[Boolean](booleanSupport)
  implicit val optionIntSupport: ValueSupport[Option[Int]] = new OptionValueSupport[Int](intSupport)
  implicit val optionLongSupport: ValueSupport[Option[Long]] = new OptionValueSupport[Long](longSupport)
  implicit val optionDoubleSupport: ValueSupport[Option[Double]] = new OptionValueSupport[Double](doubleSupport)
  implicit val optionSpatialSupport: ValueSupport[Option[SpatialPoint]] = new OptionValueSupport[SpatialPoint](spatialPointSupport)

  implicit def fv2SearchTerm[T](fv: FieldAndValue[T]): SearchTerm = fv.field.support.searchTerm(fv)

  implicit def string2ParsableSearchTerm(value: String): SearchTerm = parse(value)

  implicit class IntExtras(i: Int) {
    def meters: Length = new Length(i)
    def miles: Length = new Length(i * 1609.344)
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

  def exact[T](fv: FieldAndValue[T]): SearchTerm = fv.field.support.searchTerm(fv)
  def intRange(field: Field[Int], start: Int, end: Int): SearchTerm = new RangeIntSearchTerm(field, math.min(start, end), math.max(start, end))
  def longRange(field: Field[Long], start: Long, end: Long): SearchTerm = new RangeLongSearchTerm(field, math.min(start, end), math.max(start, end))
  def doubleRange(field: Field[Double], start: Double, end: Double): SearchTerm = new RangeDoubleSearchTerm(field, math.min(start, end), math.max(start, end))

  def intSet(field: Field[Int], set: Seq[Int]): SearchTerm = new SetIntSearchTerm(field, set)
  def longSet(field: Field[Long], set: Seq[Long]): SearchTerm = new SetLongSearchTerm(field, set)
  def doubleSet(field: Field[Double], set: Seq[Double]): SearchTerm = new SetDoubleSearchTerm(field, set)

  def regexp(fv: FieldAndValue[String]): RegexpSearchTerm = new RegexpSearchTerm(Some(fv.field), fv.value.toString)
  def regexp(value: String): RegexpSearchTerm = new RegexpSearchTerm(None, value)

  def wildcard(fv: FieldAndValue[String]): WildcardSearchTerm = new WildcardSearchTerm(Some(fv.field), fv.value.toString)
  def wildcard(value: String): WildcardSearchTerm = new WildcardSearchTerm(None, value)

  def fuzzy(value: String): FuzzySearchTerm = new FuzzySearchTerm(None, value)
  def fuzzy(fv: FieldAndValue[String]): FuzzySearchTerm = new FuzzySearchTerm(Some(fv.field), fv.value.toString)

  def phrase(fv: FieldAndValue[String]): PhraseSearchTerm = phrase(fv, 0)
  def phrase(fv: FieldAndValue[String], slop: Int): PhraseSearchTerm = new PhraseSearchTerm(Some(fv.field), fv.value, slop)
  def phrase(value: String): PhraseSearchTerm = phrase(value, 0)
  def phrase(value: String, slop: Int): PhraseSearchTerm = new PhraseSearchTerm(None, value, slop)

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

  def spatialDistance(field: Field[SpatialPoint], point: SpatialPoint, radius: Length): SpatialDistanceTerm = new SpatialDistanceTerm(field, point, radius)

  def spatialPolygon(field: Field[SpatialPoint], polygons: SpatialPolygon*): SpatialPolygonTerm = new SpatialPolygonTerm(field, polygons.toList)

  def grouped(minimumNumberShouldMatch: Int,
              entries: (SearchTerm, Condition)*): GroupedSearchTerm = GroupedSearchTerm(
    minimumNumberShouldMatch = minimumNumberShouldMatch,
    conditionalTerms = entries.toList
  )
  def grouped(entries: (SearchTerm, Condition)*): GroupedSearchTerm = grouped(0, entries: _*)

  def all(terms: SearchTerm*): GroupedSearchTerm = grouped(terms.map(t => t -> Condition.Must): _*)

  def any(terms: SearchTerm*): GroupedSearchTerm = grouped(minimumNumberShouldMatch = 1, terms.map(t => t -> Condition.Should): _*)

  def none(terms: SearchTerm*): GroupedSearchTerm = grouped(terms.map(t => t -> Condition.MustNot): _*)

  def boost(term: SearchTerm, boost: Double): BoostedSearchTerm = BoostedSearchTerm(term, boost)

  def drillDown(value: FacetValue, onlyThisLevel: Boolean = false): DrillDownSearchTerm = {
    new DrillDownSearchTerm(value.field, value.path, onlyThisLevel)
  }

  implicit class IntFieldExtras(val field: Field[Int]) extends AnyVal {
    def >=(value: Int): SearchTerm = intRange(field, value, Int.MaxValue)
    def >(value: Int): SearchTerm = intRange(field, value + 1, Int.MaxValue)
    def <=(value: Int): SearchTerm = intRange(field, Int.MinValue, value)
    def <(value: Int): SearchTerm = intRange(field, Int.MinValue, value - 1)
    def <=>(value: (Int, Int)): SearchTerm = intRange(field, value._1, value._2)
    def contains(values: Int*): SearchTerm = intSet(field, values)
  }

  implicit class LongFieldExtras(val field: Field[Long]) extends AnyVal {
    def >=(value: Long): SearchTerm = longRange(field, value, Long.MaxValue)
    def >(value: Long): SearchTerm = longRange(field, value + 1, Long.MaxValue)
    def <=(value: Long): SearchTerm = longRange(field, Long.MinValue, value)
    def <(value: Long): SearchTerm = longRange(field, Long.MinValue, value - 1)
    def <=>(value: (Long, Long)): SearchTerm = longRange(field, value._1, value._2)
    def contains(values: Long*): SearchTerm = longSet(field, values)
  }

  private val doublePrecision = 0.0001

  implicit class DoubleFieldExtras(val field: Field[Double]) extends AnyVal {
    def >=(value: Double): SearchTerm = doubleRange(field, value, Double.MaxValue)
    def >(value: Double): SearchTerm = doubleRange(field, value + doublePrecision, Double.MaxValue)
    def <=(value: Double): SearchTerm = doubleRange(field, Double.MinValue, value)
    def <(value: Double): SearchTerm = doubleRange(field, Double.MinValue, value - doublePrecision)
    def <=>(value: (Double, Double)): SearchTerm = doubleRange(field, value._1, value._2)
    def contains(values: Double*): SearchTerm = doubleSet(field, values)
  }

  implicit class SpatialFieldExtras(val field: Field[SpatialPoint]) extends AnyVal {
    def within(length: Length): SpatialPartialDistance = SpatialPartialDistance(field, length)
  }

  case class SpatialPartialDistance(field: Field[SpatialPoint], length: Length) {
    def of(point: SpatialPoint): SpatialDistanceTerm = spatialDistance(field, point, length)
  }
}