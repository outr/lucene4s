import sbtcrossproject.CrossPlugin.autoImport.crossProject

ThisBuild / name := "lucene4s"
ThisBuild / organization := "com.outr"
ThisBuild / version := "1.11.1"
ThisBuild / scalaVersion := "2.13.5"
ThisBuild / crossScalaVersions := List("2.13.5", "2.12.13", "2.11.12", "3.0.0")
scalacOptions ++= Seq("-unchecked", "-deprecation")

ThisBuild / publishTo := sonatypePublishTo.value
ThisBuild / sonatypeProfileName := "com.outr"
ThisBuild / publishMavenStyle := true
ThisBuild / licenses := Seq("MIT" -> url("https://github.com/outr/lucene4s/blob/master/LICENSE"))
ThisBuild / sonatypeProjectHosting := Some(xerial.sbt.Sonatype.GitHubHosting("outr", "lucene4s", "matt@outr.com"))
ThisBuild / homepage := Some(url("https://github.com/outr/lucene4s"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/outr/lucene4s"),
    "scm:git@github.com:outr/lucene4s.git"
  )
)
ThisBuild / developers := List(
  Developer(id="darkfrog", name="Matt Hicks", email="matt@matthicks.com", url=url("http://matthicks.com"))
)

val luceneVersion = "8.8.2"

val scalaTestVersion = "3.2.9"

lazy val root = project.in(file("."))
  .aggregate(coreJS, coreJVM, implementation)
  .settings(
    name := "lucene4s",
    publish := {},
    publishLocal := {}
  )

lazy val core = crossProject(JVMPlatform, JSPlatform)
  .in(file("core"))
  .settings(
    name := "lucene4s-core"
  )

lazy val coreJS = core.js
lazy val coreJVM = core.jvm

lazy val implementation = project
  .in(file("implementation"))
  .settings(
    name := "lucene4s",
    libraryDependencies ++= Seq(
//      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.apache.lucene" % "lucene-core" % luceneVersion,
      "org.apache.lucene" % "lucene-analyzers-common" % luceneVersion,
      "org.apache.lucene" % "lucene-queryparser" % luceneVersion,
      "org.apache.lucene" % "lucene-facet" % luceneVersion,
      "org.apache.lucene" % "lucene-highlighter" % luceneVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
    )
  )
  .dependsOn(coreJVM)