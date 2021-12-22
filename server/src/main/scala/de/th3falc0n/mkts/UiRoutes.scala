package de.th3falc0n.mkts

import cats.data.Kleisli
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.option._
import de.lolhens.http4s.spa._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import org.http4s.server.staticcontent.ResourceServiceBuilder
import org.http4s.{HttpRoutes, Uri}

class UiRoutes() {
  private val app = SinglePageApp(
    title = "Mikrotik List Importer",
    webjar = webjars.frontend.webjarAsset,
    dependencies = Seq(
      SpaDependencies.react17,
      SpaDependencies.bootstrap5,
      SpaDependencies.bootstrapIcons1,
    ),
  )

  private val appController = SinglePageAppController[IO](
    mountPoint = Uri.Root,
    controller = Kleisli.pure(app),
    resourceServiceBuilder = ResourceServiceBuilder[IO]("/assets").some
  )

  val toRoutes: HttpRoutes[IO] = {
    import org.http4s.dsl.io._
    Router(
      "/" -> appController.toRoutes,

      "/api" -> HttpRoutes.of {
        case GET -> Root / "entries" =>
          import org.http4s.circe.CirceEntityCodec._
          Ok(Seq("a", "b", "c"))
      },
    )
  }
}
