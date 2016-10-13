package com.outr

import com.outr.lucene4s.field.Field
import com.outr.lucene4s.field.value.FieldAndValue
import com.outr.lucene4s.field.value.support._
import com.outr.lucene4s.query.{Condition, FuzzySearchTerm, GroupedSearchTerm, ParsableSearchTerm, RegexpSearchTerm, SearchTerm, TermSearchTerm, WildcardSearchTerm}

import scala.language.implicitConversions

package object lucene4s {
  implicit def stringSupport: ValueSupport[String] = StringValueSupport
  implicit def intSupport: ValueSupport[Int] = IntValueSupport

  implicit def string2ParsableSearchTerm(value: String): SearchTerm = parse(value)

  def parse(field: Field[_], value: String): ParsableSearchTerm = parse(field, value, allowLeadingWildcard = false)
  def parse(field: Field[_], value: String, allowLeadingWildcard: Boolean): ParsableSearchTerm = new ParsableSearchTerm(Some(field), value, allowLeadingWildcard)
  def parse(value: String): ParsableSearchTerm = parse(value, allowLeadingWildcard = false)
  def parse(value: String, allowLeadingWildcard: Boolean): ParsableSearchTerm = new ParsableSearchTerm(None, value, allowLeadingWildcard)

  def term(fv: FieldAndValue[_]): TermSearchTerm = new TermSearchTerm(Some(fv.field), fv.value.toString)
  def term(value: String): TermSearchTerm = new TermSearchTerm(None, value)

  def regexp(fv: FieldAndValue[_]): RegexpSearchTerm = new RegexpSearchTerm(Some(fv.field), fv.value.toString)
  def regexp(value: String): RegexpSearchTerm = new RegexpSearchTerm(None, value)

  def wildcard(fv: FieldAndValue[_]): WildcardSearchTerm = new WildcardSearchTerm(Some(fv.field), fv.value.toString)
  def wildcard(value: String): WildcardSearchTerm = new WildcardSearchTerm(None, value)

  def fuzzy(value: String): FuzzySearchTerm = new FuzzySearchTerm(None, value)
  def fuzzy(fv: FieldAndValue[_]): FuzzySearchTerm = new FuzzySearchTerm(Some(fv.field), fv.value.toString)

  def grouped(disableCoord: Boolean,
              minimumNumberShouldMatch: Int,
              entries: (SearchTerm, Condition)*): GroupedSearchTerm = new GroupedSearchTerm(
    disableCoord = disableCoord,
    minimumNumberShouldMatch = minimumNumberShouldMatch,
    conditionalTerms = entries.toList
  )
  def grouped(entries: (SearchTerm, Condition)*): GroupedSearchTerm = grouped(false, 0, entries: _*)
}