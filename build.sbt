inThisBuild(Seq(
  scalaVersion := "2.13.7",
  name := "mikrotik-list-importer",
  version := "0.1"
))

name := (ThisBuild / name).value

idePackagePrefix := Some("de.th3falc0n.mkts")

Compile / mainClass := Some("de.th3falc0n.mkts.Main")

assembly / mainClass := Some("de.th3falc0n.mkts.Main")

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "2.8.0",
  "org.typelevel" %% "cats-effect" % "3.3.14",
  "com.softwaremill.sttp.client3" %% "core" % "3.7.6",
  "me.legrange" % "mikrotik" % "3.0.7",
  "com.typesafe" % "config" % "1.4.2",
  "ch.qos.logback" % "logback-classic" % "1.4.0",
  "org.slf4j" % "slf4j-api" % "2.0.0"
)
