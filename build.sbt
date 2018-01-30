name := "lucene4s"
organization := "com.outr"
version := "1.5.6"
scalaVersion := "2.12.4"
crossScalaVersions := List("2.12.4", "2.11.11", "2.13.0-M2")
parallelExecution in Test := false
fork := true
scalacOptions ++= Seq("-unchecked", "-deprecation")

val luceneVersion = "7.2.1"

val scalaTestVersion = "3.0.5"
val scalacticVersion = "3.0.5"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.apache.lucene" % "lucene-core" % luceneVersion,
  "org.apache.lucene" % "lucene-analyzers-common" % luceneVersion,
  "org.apache.lucene" % "lucene-queryparser" % luceneVersion,
  "org.apache.lucene" % "lucene-facet" % luceneVersion,
  "org.apache.lucene" % "lucene-highlighter" % luceneVersion,
  "org.scalactic" %% "scalactic" % scalaTestVersion % "test",
  "org.scalatest" %% "scalatest" % scalacticVersion % "test"
)

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oF")