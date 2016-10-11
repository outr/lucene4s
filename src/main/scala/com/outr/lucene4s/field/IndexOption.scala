package com.outr.lucene4s.field

sealed trait IndexOption

object IndexOption {
  case object Documents extends IndexOption
  case object Frequencies extends IndexOption
  case object Positions extends IndexOption
  case object Offsets extends IndexOption
}