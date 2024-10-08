ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "processor",
    idePackagePrefix := Some("org.kiuru.processor")
  )
val chiselVersion = "6.0.0"
addCompilerPlugin("org.chipsalliance" % "chisel-plugin" % chiselVersion cross CrossVersion.full)
libraryDependencies += "org.chipsalliance" %% "chisel" % chiselVersion
libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % chiselVersion
libraryDependencies ++= Seq(
  "org.apache.derby" % "derby"          % "10.16.1.1",
  "org.hibernate"    % "hibernate-core" % "6.5.2.Final"
)