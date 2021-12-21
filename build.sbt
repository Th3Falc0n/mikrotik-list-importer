name := "mikrotik-list-importer"

version := "0.1"

scalaVersion := "2.13.7"

libraryDependencies += "com.softwaremill.sttp.client3" %% "core" % "3.3.18"
libraryDependencies += "me.legrange" % "mikrotik" % "3.0.7"

libraryDependencies += "com.typesafe" % "config" % "1.4.1"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.9"
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.32"

idePackagePrefix := Some("de.th3falc0n.mkts")
