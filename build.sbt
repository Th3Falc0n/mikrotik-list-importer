inThisBuild(Seq(
  scalaVersion := "2.13.8",
  name := "mikrotik-list-importer",
  version := "0.1",
))

name := (ThisBuild / name).value

idePackagePrefix := Some("de.th3falc0n.mkts")

val V = new {
  val cats = "2.6.1"
  val catsEffect = "3.2.0"
  val circe = "0.14.1"
  val http4s = "0.23.13"
  val http4sBlazeServer = "0.23.12"
  val http4sDom = "0.2.0"
  val http4sSpa = "0.4.0"
  val lightbendConfig = "1.4.2"
  val logbackClassic = "1.2.11"
  val mikrotik = "3.0.7"
  val scalajsDom = "2.0.0"
  val scalajsReact = "2.0.0"
  val sttpClient = "3.7.0"
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
      "org.typelevel" %%% "cats-core" % V.cats,
      "org.typelevel" %%% "cats-effect" % V.catsEffect,
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
      "org.scala-js" %%% "scalajs-dom" % V.scalajsDom,
      "org.http4s" %%% "http4s-client" % V.http4s,
      "org.http4s" %%% "http4s-dom" % V.http4sDom
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
      "com.softwaremill.sttp.client3" %% "core" % V.sttpClient,
      "me.legrange" % "mikrotik" % V.mikrotik,
      "com.typesafe" % "config" % V.lightbendConfig,
      "ch.qos.logback" % "logback-classic" % V.logbackClassic,
      "de.lolhens" %% "http4s-spa" % V.http4sSpa,
      "org.http4s" %% "http4s-blaze-server" % V.http4sBlazeServer,
      "org.http4s" %% "http4s-dsl" % V.http4s,
      "org.http4s" %% "http4s-server" % V.http4s,
    )
  )
