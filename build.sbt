name := "lucene4s"
organization := "com.outr"
version := "1.7.0-SNAPSHOT"
scalaVersion := "2.12.6"
crossScalaVersions := List("2.12.6", "2.11.12", "2.13.0-M2")
parallelExecution in Test := false
fork := true
scalacOptions ++= Seq("-unchecked", "-deprecation")

publishTo in ThisBuild := sonatypePublishTo.value
sonatypeProfileName in ThisBuild := "com.outr"
publishMavenStyle in ThisBuild := true
licenses in ThisBuild := Seq("MIT" -> url("https://github.com/outr/lucene4s/blob/master/LICENSE"))
sonatypeProjectHosting in ThisBuild := Some(xerial.sbt.Sonatype.GitHubHosting("outr", "lucene4s", "matt@outr.com"))
homepage in ThisBuild := Some(url("https://github.com/outr/lucene4s"))
scmInfo in ThisBuild := Some(
  ScmInfo(
    url("https://github.com/outr/lucene4s"),
    "scm:git@github.com:outr/lucene4s.git"
  )
)
developers in ThisBuild := List(
  Developer(id="darkfrog", name="Matt Hicks", email="matt@matthicks.com", url=url("http://matthicks.com"))
)

val luceneVersion = "7.3.0"
val powerScalaVersion = "2.0.5"

val scalaTestVersion = "3.0.5"
val scalacticVersion = "3.0.5"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.apache.lucene" % "lucene-core" % luceneVersion,
  "org.apache.lucene" % "lucene-analyzers-common" % luceneVersion,
  "org.apache.lucene" % "lucene-queryparser" % luceneVersion,
  "org.apache.lucene" % "lucene-facet" % luceneVersion,
  "org.apache.lucene" % "lucene-highlighter" % luceneVersion,
  "org.powerscala" %% "powerscala-io" % powerScalaVersion,
  "org.scalactic" %% "scalactic" % scalaTestVersion % "test",
  "org.scalatest" %% "scalatest" % scalacticVersion % "test"
)

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oF")