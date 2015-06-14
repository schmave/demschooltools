name := "DemSchoolTools"

version := "1.1"

lazy val root = (project in file(".")).enablePlugins(PlayJava, PlayEbean)

scalaVersion := "2.11.6"

// javacOptions in Compile ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

libraryDependencies ++= Seq(
  javaJdbc,
  evolutions,
  cache,
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "be.objectify"  %% "deadbolt-java"     % "2.3.2",
  "com.feth"      %% "play-authenticate" % "0.6.8",
  "com.typesafe.play" %% "play-mailer" % "2.4.0-RC1",
  "org.avaje.ebeanorm" % "avaje-ebeanorm-api" % "3.1.1",
  "com.ecwid" % "ecwid-mailchimp" % "2.0.1.0",
  "org.xhtmlrenderer" % "flying-saucer-pdf" % "9.0.7"
)
