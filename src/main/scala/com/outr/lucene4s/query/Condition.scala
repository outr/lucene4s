package com.outr.lucene4s.query

import org.apache.lucene.search.BooleanClause

sealed abstract class Condition(private[lucene4s] val occur: BooleanClause.Occur)

object Condition {
  case object Must extends Condition(BooleanClause.Occur.MUST)
  case object Filter extends Condition(BooleanClause.Occur.FILTER)
  case object Should extends Condition(BooleanClause.Occur.SHOULD)
  case object MustNot extends Condition(BooleanClause.Occur.MUST_NOT)
}