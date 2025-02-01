import sbtcrossproject.CrossPlugin.autoImport.crossProject

ThisBuild / organization       := "com.outr"
ThisBuild / version            := "1.11.1"
ThisBuild / scalaVersion       := "2.13.16"
ThisBuild / crossScalaVersions := List("2.13.16", "2.12.20", "2.11.12", "3.6.3")
scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Wunused:all")

ThisBuild / publishTo           := sonatypePublishTo.value
ThisBuild / sonatypeProfileName := "com.outr"
ThisBuild / licenses := Seq(
  "MIT" -> url("https://github.com/outr/lucene4s/blob/master/LICENSE")
)
ThisBuild / sonatypeProjectHosting := Some(
  xerial.sbt.Sonatype.GitHubHosting("outr", "lucene4s", "matt@outr.com")
)
ThisBuild / homepage := Some(url("https://github.com/outr/lucene4s"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/outr/lucene4s"),
    "scm:git@github.com:outr/lucene4s.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id = "darkfrog",
    name = "Matt Hicks",
    email = "matt@matthicks.com",
    url = url("http://matthicks.com")
  )
)

val luceneVersion = "10.1.0"

val scalaTestVersion = "3.2.19"

lazy val root = project
  .in(file("."))
  .aggregate(coreJS, coreJVM, implementation)
  .settings(
    name         := "lucene4s",
    publish      := {},
    publishLocal := {}
  )

lazy val core = crossProject(JVMPlatform, JSPlatform)
  .in(file("core"))
  .settings(
    name := "lucene4s-core"
  )

lazy val coreJS  = core.js
lazy val coreJVM = core.jvm

lazy val implementation = project
  .in(file("implementation"))
  .settings(
    name := "lucene4s",
    libraryDependencies ++= Seq(
//      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.apache.lucene" % "lucene-core"            % luceneVersion,
      "org.apache.lucene" % "lucene-analysis-common" % luceneVersion,
      "org.apache.lucene" % "lucene-queryparser"     % luceneVersion,
      "org.apache.lucene" % "lucene-facet"           % luceneVersion,
      "org.apache.lucene" % "lucene-highlighter"     % luceneVersion,
      "org.scalatest"    %% "scalatest"              % scalaTestVersion % Test
    )
  )
  .dependsOn(coreJVM)
