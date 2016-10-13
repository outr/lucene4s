package com.outr.lucene4s.query

import com.outr.lucene4s.Lucene
import com.outr.lucene4s.field.Field
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{BooleanClause, BooleanQuery, FuzzyQuery, MatchAllDocsQuery, Query, RegexpQuery, TermQuery, WildcardQuery}
import org.apache.lucene.util.automaton.RegExp

sealed trait SearchTerm {
  protected[lucene4s] def toLucene(lucene: Lucene): Query
}

object MatchAllSearchTerm extends SearchTerm {
  private lazy val instance = new MatchAllDocsQuery

  override protected[lucene4s] def toLucene(lucene: Lucene): Query = instance
}

class ParsableSearchTerm(field: Option[Field[_]], value: String, allowLeadingWildcard: Boolean) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = {
    val parser = new QueryParser(field.getOrElse(lucene.fullText).name, lucene.standardAnalyzer)
    parser.setAllowLeadingWildcard(allowLeadingWildcard)
    parser.parse(value)
  }
}

class TermSearchTerm(field: Option[Field[_]], value: String) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = new TermQuery(new Term(field.getOrElse(lucene.fullText).name, value))
}

class RegexpSearchTerm(field: Option[Field[_]], value: String) extends SearchTerm {
  // TODO: add support for regular expression flags
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = new RegexpQuery(new Term(field.getOrElse(lucene.fullText).name, value), RegExp.ALL)
}

class WildcardSearchTerm(field: Option[Field[_]], value: String) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = new WildcardQuery(new Term(field.getOrElse(lucene.fullText).name, value))
}

class FuzzySearchTerm(field: Option[Field[_]], value: String) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = new FuzzyQuery(new Term(field.getOrElse(lucene.fullText).name, value))
}

class GroupedSearchTerm(disableCoord: Boolean,
                        minimumNumberShouldMatch: Int,
                        conditionalTerms: List[(SearchTerm, Condition)]) extends SearchTerm {
  override protected[lucene4s] def toLucene(lucene: Lucene): Query = {
    val b = new BooleanQuery.Builder
    b.setDisableCoord(disableCoord)
    b.setMinimumNumberShouldMatch(minimumNumberShouldMatch)
    conditionalTerms.map {
      case (st, c) => b.add(st.toLucene(lucene), c.occur)
    }
    b.build()
  }
}