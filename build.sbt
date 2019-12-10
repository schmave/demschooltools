name := "DemSchoolTools"

version := "1.1"

lazy val root = (project in file("."))
	.settings(
		publishArtifact in (Compile, packageDoc) := false,
		publishArtifact in packageDoc := false,
		sources in (Compile,doc) := Seq.empty
	    )
	.enablePlugins(PlayJava, PlayEbean)

scalaVersion := "2.11.7"

// javacOptions in Compile ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

pipelineStages := Seq(digest, gzip)

// resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  javaJdbc,
  evolutions,
  cache,
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "com.feth"      %% "play-authenticate" % "0.8.3",
  "com.typesafe.play" %% "play-mailer" % "5.0.0",
  "com.ecwid" % "ecwid-mailchimp" % "2.0.1.0",
  "org.xhtmlrenderer" % "flying-saucer-pdf-itext5" % "9.1.19",
  "com.github.spullara.mustache.java" % "compiler" % "0.9.2",
  "org.apache.poi" % "poi-ooxml" % "3.17",
  "org.mindrot" % "jbcrypt" % "0.3m"
)

// Disable javadoc
sources in (Compile, doc) := Seq.empty
publishArtifact in (Compile, packageDoc) := false

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

dist <<= dist dependsOn webpack

stage <<= stage dependsOn webpack

PlayKeys.playRunHooks <+= baseDirectory.map(Webpack.apply)
