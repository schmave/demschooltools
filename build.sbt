import RjsKeys._
import WebJs._

name := "DemSchoolTools"

version := "1.1"

lazy val root = (project in file("."))
	.settings(
		publishArtifact in (Compile, packageDoc) := false,
		publishArtifact in packageDoc := false,
		sources in (Compile,doc) := Seq.empty
	    )
	.enablePlugins(PlayJava, PlayEbean)

scalaVersion := "2.11.6"

// javacOptions in Compile ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

pipelineStages := Seq(rjs, digest, gzip)

RjsKeys.mainModule := "utils"

libraryDependencies ++= Seq(
  javaJdbc,
  evolutions,
  cache,
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "com.feth"      %% "play-authenticate" % "0.7.0",
  "com.typesafe.play" %% "play-mailer" % "3.0.1",
  "com.ecwid" % "ecwid-mailchimp" % "2.0.1.0",
  "org.xhtmlrenderer" % "flying-saucer-pdf" % "9.0.7",
  "com.googlecode.usc" % "jdbcdslog" % "1.0.6.2"
)

