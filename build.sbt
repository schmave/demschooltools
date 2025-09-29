import scala.sys.process._

name := "DemSchoolTools"
ThisBuild / scalaVersion := "2.13.16"
ThisBuild / version := "1.1"
ThisBuild / javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

lazy val authLibrary = (project in file("authLibrary"))
  .enablePlugins(PlayJava)
  .settings(
    libraryDependencies ++= Seq(
      "org.apache.httpcomponents" % "httpclient" % "4.5.14",
      "org.apache.commons" % "commons-lang3" % "3.12.0",
      javaWs,
      ehcache
    )
  )

lazy val modelsLibrary = (project in file("modelsLibrary"))
  .enablePlugins(PlayJava, PlayEbean)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-mailer" % "8.0.1",
      "org.projectlombok" % "lombok" % "1.18.26"
    )
  )
  .aggregate(authLibrary)
  .dependsOn(authLibrary)

lazy val root = (project in file("."))
  .enablePlugins(PlayJava, PlayEbean)
  .settings(
    libraryDependencies ++= Seq(
      "org.apache.httpcomponents.client5" % "httpclient5" % "5.4.2"
    )
  )
  .aggregate(modelsLibrary, authLibrary)
  .dependsOn(modelsLibrary, authLibrary)

authLibrary / Compile / doc / sources := Seq.empty
authLibrary / Compile / packageDoc / publishArtifact := false
modelsLibrary / Compile / doc / sources := Seq.empty
modelsLibrary / Compile / packageDoc / publishArtifact := false
root / Compile / doc / sources := Seq.empty
root / Compile / packageDoc / publishArtifact := false

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
  "org.xhtmlrenderer" % "flying-saucer-pdf-itext5" % "9.1.22",
  "com.github.spullara.mustache.java" % "compiler" % "0.9.10",
  "org.apache.poi" % "poi-ooxml" % "5.2.2",
  "org.mindrot" % "jbcrypt" % "0.4",
  "com.rollbar" % "rollbar-java" % "1.10.0"
)

// Run webpack
lazy val webpack = taskKey[Unit]("Run webpack when packaging the application")

def runWebpack(file: File) = {
  val command = Seq("npm", "run", "compile")
  val os = sys.props("os.name").toLowerCase
  val makeCmd = os match {
    case x if x contains "windows" => Seq("cmd", "/C") ++ command
    case _                         => command
  }
  makeCmd.!
}

webpack := {
  if (runWebpack(baseDirectory.value) != 0)
    throw new Exception("Something went wrong when running webpack.")
}

dist := (dist dependsOn webpack).value
stage := (stage dependsOn webpack).value
PlayKeys.playRunHooks += baseDirectory.map(Webpack.apply).value

Global / onChangedBuildSource := ReloadOnSourceChanges
