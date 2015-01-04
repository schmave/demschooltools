name := "DemSchoolTools"

version := "1.1"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

// javacOptions in Compile ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
    "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
      "be.objectify"  %% "deadbolt-java"     % "2.3.2",
	"com.feth"      %% "play-authenticate" % "0.6.8",
	// "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.0"
        "com.typesafe.play" %% "play-mailer" % "2.4.0-RC1",
	"org.avaje.ebeanorm" % "avaje-ebeanorm-api" % "3.1.1"
)
