package com.outr.lucene4s

trait Stringify[T] {
  def toString(value: T): String
  def fromString(s: String): T
}

object Stringify {
  def apply[T](from: String => T, to: T => String = (t: T) => t.toString): Stringify[T] = new Stringify[T] {
    override def toString(value: T): String = to(value)

    override def fromString(s: String): T = from(s)
  }
}