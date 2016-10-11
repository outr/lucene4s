package com.outr.lucene4s.field

sealed abstract class DocValuesType(private[lucene4s] val value: org.apache.lucene.index.DocValuesType)

object DocValuesType {
  import org.apache.lucene.index.{DocValuesType => DVT}

  case object None extends DocValuesType(DVT.NONE)
  case object Numeric extends DocValuesType(DVT.NUMERIC)
  case object Binary extends DocValuesType(DVT.BINARY)
  case object Sorted extends DocValuesType(DVT.SORTED)
  case object SortedNumeric extends DocValuesType(DVT.SORTED_NUMERIC)
  case object SortedSet extends DocValuesType(DVT.SORTED_SET)
}