// Scala versions
val scala213 = "2.13.16"
val scala3 = "3.6.3"
val scala2 = List(scala213)
val allScalaVersions = scala3 :: scala2

// Variables
val org: String = "com.outr"
val projectName: String = "lucene4s"
val githubOrg: String = "outr"
val email: String = "matt@matthicks.com"
val developerId: String = "darkfrog"
val developerName: String = "Matt Hicks"
val developerURL: String = "https://matthicks.com"

name := projectName
ThisBuild / organization := org
ThisBuild / version := "1.12.0"
ThisBuild / scalaVersion := scala213
ThisBuild / crossScalaVersions := allScalaVersions
ThisBuild / scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
ThisBuild / publishTo := sonatypePublishToBundle.value
ThisBuild / sonatypeProfileName := org
ThisBuild / licenses := Seq("MIT" -> url(s"https://github.com/$githubOrg/$projectName/blob/master/LICENSE"))
ThisBuild / sonatypeProjectHosting := Some(xerial.sbt.Sonatype.GitHubHosting(githubOrg, projectName, email))
ThisBuild / homepage := Some(url(s"https://github.com/$githubOrg/$projectName"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url(s"https://github.com/$githubOrg/$projectName"),
    s"scm:git@github.com:$githubOrg/$projectName.git"
  )
)
ThisBuild / developers := List(
  Developer(id=developerId, name=developerName, email=email, url=url(developerURL))
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
