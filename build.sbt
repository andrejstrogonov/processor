ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "processor",
    idePackagePrefix := Some("org.kiuru.processor")
  )
val chiselVersion = "6.0.0"
addCompilerPlugin("org.chipsalliance" % "chisel-plugin" % chiselVersion cross CrossVersion.full)
libraryDependencies += "org.chipsalliance" %% "chisel" % "6.5.0"
libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % chiselVersion
// sv2chisel was first published in 2021, on new sonatype servers hence requiring non default resolvers
resolvers ++= Seq(
  "New Sonatype Snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots/",
  "New Sonatype Releases" at "https://s01.oss.sonatype.org/service/local/repositories/releases/content/",
)
// For simpler usage, sv2chisel minor version is aligned on chisel stack minor version: x.5.x
libraryDependencies += "com.ovhcloud" %% "sv2chisel-helpers" % "0.5.0"