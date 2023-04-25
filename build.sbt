import scala.sys.process._

name := "DemSchoolTools"
ThisBuild / scalaVersion := "2.13.10"
ThisBuild / version := "1.1"
ThisBuild / javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

// I think these aren't working yet
ThisBuild / Compile / doc / sources := Seq.empty
ThisBuild / Compile / packageDoc / publishArtifact := false

lazy val authLibrary = (project in file("authLibrary")).enablePlugins(PlayJava)
  .settings(
  libraryDependencies ++= Seq(
    "org.apache.httpcomponents" % "httpclient" % "4.5.14",
    "org.apache.commons" % "commons-lang3" % "3.12.0",
    javaWs,
    ehcache,
  )
  )

lazy val modelsLibrary = (project in file("modelsLibrary")).enablePlugins(PlayJava, PlayEbean)
.settings(
  libraryDependencies ++= Seq(
    "com.typesafe.play" %% "play-mailer" % "8.0.1",
  )
).aggregate(authLibrary)
  .dependsOn(authLibrary)


lazy val root = (project in file("."))
	.enablePlugins(PlayJava, PlayEbean)
  .aggregate(modelsLibrary, authLibrary)
  .dependsOn(modelsLibrary, authLibrary)

javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")
pipelineStages := Seq(digest, gzip)
ThisBuild / Compile / playEbeanModels := Seq("models.*")

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
