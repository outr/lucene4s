package com.outr.lucene4s.query

import com.outr.lucene4s.Lucene
import com.outr.lucene4s.field.Field
import org.apache.lucene.document.{DoublePoint, IntPoint, LongPoint}
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{BooleanQuery, FuzzyQuery, MatchAllDocsQuery, Query, RegexpQuery, TermQuery, WildcardQuery}
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
    val parser = new QueryParser(field.getOrElse(lucene.fullText).name, lucene.standardAnalyzer)
    parser.setAllowLeadingWildcard(allowLeadingWildcard)
    parser.parse(value)
  }

  override def toString: String = s"parse($field, value: $value, allowLeadingWildcard: $allowLeadingWildcard)"
}

class TermSearchTerm(field: Option[Field[String]], value: String) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = new TermQuery(new Term(field.getOrElse(lucene.fullText).name, value))

  override def toString: String = s"term(${field.map(_.name)} = $value)"
}

class ExactIntSearchTerm(field: Field[Int], value: Int) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = IntPoint.newExactQuery(field.name, value)

  override def toString: String = s"term(${field.name} = $value)"
}

class ExactLongSearchTerm(field: Field[Long], value: Long) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = LongPoint.newExactQuery(field.name, value)

  override def toString: String = s"term(${field.name} = $value)"
}

class ExactDoubleSearchTerm(field: Field[Double], value: Double) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = DoublePoint.newExactQuery(field.name, value)

  override def toString: String = s"term(${field.name} = $value)"
}

class RegexpSearchTerm(field: Option[Field[String]], value: String) extends SearchTerm {
  // TODO: add support for regular expression flags
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = new RegexpQuery(new Term(field.getOrElse(lucene.fullText).name, value), RegExp.ALL)

  override def toString: String = s"regexp(${field.map(_.name)}, value: $value)"
}

class WildcardSearchTerm(field: Option[Field[String]], value: String) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = new WildcardQuery(new Term(field.getOrElse(lucene.fullText).name, value))

  override def toString: String = s"wildcard(${field.map(_.name)}, value: $value)"
}

class FuzzySearchTerm(field: Option[Field[String]], value: String) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = new FuzzyQuery(new Term(field.getOrElse(lucene.fullText).name, value))

  override def toString: String = s"fuzzy(${field.map(_.name)}, value: $value)"
}

class GroupedSearchTerm(disableCoord: Boolean,
                        minimumNumberShouldMatch: Int,
                        conditionalTerms: List[(SearchTerm, Condition)]) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = {
    val b = new BooleanQuery.Builder
    b.setDisableCoord(disableCoord)
    b.setMinimumNumberShouldMatch(minimumNumberShouldMatch)
    conditionalTerms.foreach {
      case (st, c) => b.add(st.toLucene(lucene), c.occur)
    }
    b.build()
  }

  override def toString: String = s"grouped(disableCoord: $disableCoord, minimumNumberShouldMatch: $minimumNumberShouldMatch, conditionalTerms: ${conditionalTerms.map(ct => s"${ct._1} -> ${ct._2}").mkString(", ")})"
}