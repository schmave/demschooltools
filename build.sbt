import scala.sys.process._

scalaVersion := "2.13.10"
name := "DemSchoolTools"

version := "1.1"

lazy val root = (project in file("."))
//	.settings(
//		publishArtifact / (Compile, packageDoc) := false,
//		publishArtifact / packageDoc := false,
//		sources / (Compile,doc) := Seq.empty
//	    )
	.enablePlugins(PlayJava, PlayEbean)


javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

pipelineStages := Seq(digest, gzip)

//resolvers += Resolver.bintrayRepo("playframework", "maven")

// These settings from
//  https://github.com/playframework/play-ebean/blob/main/docs/manual/working/javaGuide/main/sql/code/ebean.sbt
Compile / playEbeanModels := Seq("models.*")
//playEbeanDebugLevel := 2

libraryDependencies ++= Seq(
  javaJdbc,
  evolutions,
  ehcache,
  guice,
  "org.postgresql" % "postgresql" % "42.5.4",
  "com.typesafe.play" %% "play-mailer" % "8.0.1",
  "com.typesafe.play" %% "play-mailer-guice" % "8.0.1",
  "com.ecwid" % "ecwid-mailchimp" % "2.0.1.0",
  "org.xhtmlrenderer" % "flying-saucer-pdf-itext5" % "9.1.22",
  "com.github.spullara.mustache.java" % "compiler" % "0.9.10",
  "org.apache.poi" % "poi-ooxml" % "5.2.2",
  "org.mindrot" % "jbcrypt" % "0.4",

  // play-authenticate stuff
  "org.apache.httpcomponents" % "httpclient" % "4.5.14",
  "org.apache.commons" % "commons-lang3" % "3.12.0",
  javaWs,
)

// Run webpack
lazy val webpack = taskKey[Unit]("Run webpack when packaging the application")

def runWebpack(file: File) = {
  val command = Seq("npm", "run", "compile")
  val os = sys.props("os.name").toLowerCase
  val makeCmd = os match {
    case x if x contains "windows" => Seq("cmd", "/C") ++ command
    case _ => command
  }
  makeCmd.!
}

webpack := {
  if(runWebpack(baseDirectory.value) != 0) throw new Exception("Something went wrong when running webpack.")
}

dist := (dist dependsOn webpack).value

stage := (stage dependsOn webpack).value

PlayKeys.playRunHooks += baseDirectory.map(Webpack.apply).value

Global / onChangedBuildSource := ReloadOnSourceChanges