package tests

import java.util.UUID

import org.scalatest.concurrent.Eventually
import org.scalatest.{Matchers, WordSpec}

import scala.util.Try
import com.outr.lucene4s._
import com.outr.lucene4s.field.{Field, FieldType}

class TokenSpec extends WordSpec with Matchers with Eventually {
  val lucene: Lucene = new DirectLucene(uniqueFields = List.empty, defaultFullTextSearchable = true, autoCommit = true)

  val sessionTokenField: Field[String] = lucene.create.field[String](name = "sessionTokenField", FieldType.Untokenized)
  val sessionEmailField: Field[String] = lucene.create.field[String](name = "sessionEmailField")
  val sessionExpireField: Field[Long] = lucene.create.field[Long](name = "sessionExpireField")

  "generate session tokens for emails, expire tokens and assert that expire time is set" in {
    val userEmails = (1 to 1000) map (i => s"email$i@email.com")

    // Generate UUID -> e-mail tuples and index them
    val tokensAndEmails: Seq[(String, String)] = userEmails map { email =>
      val token = UUID.randomUUID().toString.replaceAllLiterally("-", "")
      lucene.doc().fields(sessionTokenField(token), sessionEmailField(email)).index()
      (token, email)
    }

    // Update all the tokens to be expired
    tokensAndEmails foreach {
      case (token, email) => lucene
        .update(sessionTokenField(token))
        .fields(
          sessionTokenField(token),
          sessionEmailField(email),
          sessionExpireField(System.currentTimeMillis())
        ).index()
    }
    lucene.commit()

    // Query each token and verify values are correct
    tokensAndEmails foreach {
      case (token, email) => {
//        val searchTerm = sessionTokenField(token.replaceAllLiterally("-", ""))
        val searchTerm = parse(sessionTokenField, token.replaceAllLiterally("-", ""))
        val results = lucene.query().filter(searchTerm).search().results
        results should have size 1
        val headResult = results.head

        headResult(sessionTokenField) shouldBe token
        headResult(sessionEmailField) shouldBe email
        Try(headResult(sessionExpireField)).toOption shouldBe defined
      }
    }
  }
}