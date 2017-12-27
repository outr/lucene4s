package com.outr.lucene4s.query

import org.apache.lucene.queries.mlt.MoreLikeThis

case class MoreLikeThisConfig(minTermFreq: Int = MoreLikeThis.DEFAULT_MIN_TERM_FREQ,
                              minDocFreq: Int = MoreLikeThis.DEFAULT_MIN_DOC_FREQ,
                              maxDocFreq: Int = MoreLikeThis.DEFAULT_MAX_DOC_FREQ,
                              boost: Boolean = MoreLikeThis.DEFAULT_BOOST,
                              minWordLen: Int = MoreLikeThis.DEFAULT_MIN_WORD_LENGTH,
                              maxWordLen: Int = MoreLikeThis.DEFAULT_MAX_WORD_LENGTH,
                              maxQueryTerms: Int = MoreLikeThis.DEFAULT_MAX_QUERY_TERMS
                             )

object MoreLikeThisConfig {
  implicit val default: MoreLikeThisConfig = MoreLikeThisConfig()
}