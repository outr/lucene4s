name := "lucene4s"
organization := "com.outr"
version := "1.5.6-SNAPSHOT"
scalaVersion := "2.12.4"
crossScalaVersions := List("2.12.4", "2.11.11")
parallelExecution in Test := false
fork := true
scalacOptions ++= Seq("-unchecked", "-deprecation")

val luceneVersion = "7.2.1"
val akkaVersion = "2.5.9"
val squantsVersion = "1.3.0"
val scalaTestVersion = "3.0.4"
val scalacticVersion = "3.0.4"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.apache.lucene" % "lucene-core" % luceneVersion,
  "org.apache.lucene" % "lucene-analyzers-common" % luceneVersion,
  "org.apache.lucene" % "lucene-queryparser" % luceneVersion,
  "org.apache.lucene" % "lucene-facet" % luceneVersion,
  "org.apache.lucene" % "lucene-highlighter" % luceneVersion,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "org.typelevel" %% "squants"  % squantsVersion,
  "org.scalactic" %% "scalactic" % scalaTestVersion % "test",
  "org.scalatest" %% "scalatest" % scalacticVersion % "test"
)

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oF")