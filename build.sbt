inThisBuild(Seq(
  scalaVersion := "2.13.7",
  name := "mikrotik-list-importer",
  version := "0.1",
))

name := (ThisBuild / name).value

idePackagePrefix := Some("de.th3falc0n.mkts")

val V = new {
  val circe = "0.14.1"
  val http4s = "0.23.13"
  val scalajsReact = "2.0.0"
}

lazy val root = project.in(file("."))
  .settings(
    publishArtifact := false
  )
  .aggregate(server)

lazy val common = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % V.circe,
      "io.circe" %%% "circe-generic" % V.circe,
      "io.circe" %%% "circe-parser" % V.circe,
      "org.typelevel" %%% "cats-core" % "2.6.1",
      "org.typelevel" %%% "cats-effect" % "3.2.0",
      "org.http4s" %%% "http4s-circe" % V.http4s
    )
  )

lazy val commonJvm = common.jvm
lazy val commonJs = common.js

lazy val frontend = project
  .enablePlugins(ScalaJSWebjarPlugin)
  .dependsOn(commonJs)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.japgolly.scalajs-react" %%% "core-bundle-cats_effect" % V.scalajsReact,
      "com.github.japgolly.scalajs-react" %%% "extra" % V.scalajsReact,
      "org.scala-js" %%% "scalajs-dom" % "2.0.0",
      "org.http4s" %%% "http4s-client" % V.http4s,
      "org.http4s" %%% "http4s-dom" % "0.2.0"
    ),

    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
    },
    scalaJSUseMainModuleInitializer := true,
  )

lazy val frontendWebjar = frontend.webjar
  .settings(
    webjarAssetReferenceType := Some("http4s"),
    libraryDependencies += "org.http4s" %% "http4s-server" % V.http4s
  )

lazy val server = project
  .dependsOn(commonJvm, frontendWebjar)
  .settings(
    Compile / mainClass := Some("de.th3falc0n.mkts.Main"),
    assembly / mainClass := Some("de.th3falc0n.mkts.Main"),
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "core" % "3.3.18",
      "me.legrange" % "mikrotik" % "3.0.7",
      "com.typesafe" % "config" % "1.4.1",
      "ch.qos.logback" % "logback-classic" % "1.2.9",
      "org.slf4j" % "slf4j-api" % "1.7.32",
      "de.lolhens" %% "http4s-spa" % "0.2.1",
      "org.http4s" %% "http4s-blaze-server" % V.http4s,
      "org.http4s" %% "http4s-dsl" % V.http4s
    )
  )
