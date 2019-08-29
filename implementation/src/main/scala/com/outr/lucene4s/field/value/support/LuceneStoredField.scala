package com.outr.lucene4s.field.value.support

import org.apache.lucene.document.{FieldType, StoredField}

class LuceneStoredField(name: String, `type`: FieldType, value: AnyRef) extends StoredField(name, `type`) {
  fieldsData = value
}