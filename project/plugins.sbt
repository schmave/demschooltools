// Comment to get more information during initialization
logLevel := Level.Warn
evictionErrorLevel := Level.Warn

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.18")
addSbtPlugin("com.typesafe.play" % "sbt-play-ebean" % "6.2.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.3")
addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.1.2")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")

// This lets us run the "eclipse" command at the sbt shell
// in order to generate files that are used by the VS Code Java
// language server.
addSbtPlugin("com.github.sbt" % "sbt-eclipse" % "6.0.0")

// Load environment variables defined in .env
addSbtPlugin("nl.gn0s1s" % "sbt-dotenv" % "3.2.0")