package com.outr.lucene4s.field

object FieldType {
  val NotStored = FieldType(
    indexOptions = Set(IndexOption.Documents, IndexOption.Frequencies, IndexOption.Positions),
    tokenized = true,
    frozen = true
  )
  val Stored = FieldType(
    indexOptions = Set(IndexOption.Documents, IndexOption.Frequencies, IndexOption.Positions),
    tokenized = true,
    stored = true,
    frozen = true
  )
}

case class FieldType(stored: Boolean = false,
                     tokenized: Boolean = true,
                     storeTermVectors: Boolean = false,
                     storeTermVectorOffsets: Boolean = false,
                     storeTermVectorPositions: Boolean = false,
                     storeTermVectorPayloads: Boolean = false,
                     omitNorms: Boolean = false,
                     indexOptions: Set[IndexOption] = Set.empty,
                     frozen: Boolean = false,
                     docValuesType: DocValuesType = DocValuesType.None,
                     dimensionCount: Int = 0,
                     dimensionNumBytes: Int = 0) {
  private[lucene4s] def lucene(): org.apache.lucene.document.FieldType = {
    val ft = new org.apache.lucene.document.FieldType
    ft.setStored(stored)
    ft.setTokenized(tokenized)
    ft.setStoreTermVectors(storeTermVectors)
    ft.setStoreTermVectorOffsets(storeTermVectorOffsets)
    ft.setStoreTermVectorPositions(storeTermVectorPositions)
    ft.setStoreTermVectorPayloads(storeTermVectorPayloads)
    ft.setOmitNorms(omitNorms)

    import org.apache.lucene.index.{IndexOptions => IO}
    val io = if (indexOptions.contains(IndexOption.Offsets)) {
      IO.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS
    } else if (indexOptions.contains(IndexOption.Positions)) {
      IO.DOCS_AND_FREQS_AND_POSITIONS
    } else if (indexOptions.contains(IndexOption.Frequencies)) {
      IO.DOCS_AND_FREQS
    } else if (indexOptions.contains(IndexOption.Documents)) {
      IO.DOCS
    } else {
      IO.NONE
    }
    ft.setIndexOptions(io)
    ft.setDocValuesType(docValuesType.value)
    ft.setDimensions(dimensionCount, dimensionNumBytes)
    if (frozen) ft.freeze()
    ft
  }
}