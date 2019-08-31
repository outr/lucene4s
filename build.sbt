import sbtcrossproject.CrossPlugin.autoImport.crossProject

name in ThisBuild := "lucene4s"
organization in ThisBuild := "com.outr"
version in ThisBuild := "1.9.1"
scalaVersion in ThisBuild := "2.13.0"
crossScalaVersions in ThisBuild := List("2.13.0", "2.12.8", "2.11.12")
parallelExecution in Test in ThisBuild := false
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
testOptions in Test in ThisBuild += Tests.Argument("-oD")

val luceneVersion = "8.2.0"

val scalaTestVersion = "3.1.0-SNAP13"

lazy val root = project.in(file("."))
  .aggregate(coreJS, coreJVM, implementation)
  .settings(
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
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.apache.lucene" % "lucene-core" % luceneVersion,
      "org.apache.lucene" % "lucene-analyzers-common" % luceneVersion,
      "org.apache.lucene" % "lucene-queryparser" % luceneVersion,
      "org.apache.lucene" % "lucene-facet" % luceneVersion,
      "org.apache.lucene" % "lucene-highlighter" % luceneVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
    ),
    testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oF")
  )
  .dependsOn(coreJVM)