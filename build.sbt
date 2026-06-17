ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "processor",
    idePackagePrefix := Some("org.kiuru.processor")
  )

// Chisel settings
val chiselVersion = "6.0.0"
addCompilerPlugin("org.chipsalliance" % "chisel-plugin" % chiselVersion cross CrossVersion.full)

libraryDependencies ++= Seq(
  "org.chipsalliance" %% "chisel" % "6.5.0",
  "edu.berkeley.cs" %% "chiseltest" % chiselVersion,
  "com.ovhcloud" %% "sv2chisel-helpers" % "0.5.0",
  "com.ovhcloud" %% "sv2chisel" % "0.1.0-SNAPSHOT",
  "org.yaml" % "snakeyaml" % "2.2",
  "com.github.scopt" %% "scopt" % "4.1.0",
  "org.scalatest" %% "scalatest" % "3.2.17" % Test,
  "org.openjfx" % "javafx-controls" % "17.0.2",
  "org.openjfx" % "javafx-fxml" % "17.0.2"
)

// Resolvers for sv2chisel
resolvers ++= Seq(
  "New Sonatype Snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots/",
  "New Sonatype Releases" at "https://s01.oss.sonatype.org/service/local/repositories/releases/content/"
)

// JavaFX settings
fork := true
javaOptions += "--module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml"
