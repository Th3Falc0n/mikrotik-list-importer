inThisBuild(Seq(
  scalaVersion := "2.13.10",
  name := "mikrotik-list-importer",
  version := "0.1"
))

name := (ThisBuild / name).value

idePackagePrefix := Some("de.th3falc0n.mkts")

Compile / mainClass := Some("de.th3falc0n.mkts.Main")

assembly / mainClass := Some("de.th3falc0n.mkts.Main")

assembly / assemblyOption := (assembly / assemblyOption).value.withIncludeScala(false)

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "2.9.0",
  "org.typelevel" %% "cats-effect" % "3.4.5",
  "me.legrange" % "mikrotik" % "3.0.7",
  "com.typesafe" % "config" % "1.4.2",
  "org.slf4j" % "slf4j-api" % "2.0.6",
  "ch.qos.logback" % "logback-classic" % "1.4.5"
)

val http4sVersion = "0.23.13"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-dsl" % http4sVersion
)

ThisBuild / assemblyMergeStrategy := {
  case "module-info.class" => MergeStrategy.discard
  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}
