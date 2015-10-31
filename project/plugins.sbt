// Comment to get more information during initialization
logLevel := Level.Warn

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.3")
addSbtPlugin("com.typesafe.sbt" % "sbt-play-ebean" % "1.0.0")

// I can't figure out how to disable jshint for
// jquery, handlebars, bootstrap--maybe webjars would be a solution?
// addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.7")
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.6")
