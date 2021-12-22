package de.th3falc0n.mkts

import cats.data.Kleisli
import cats.effect.IO
import cats.syntax.option._
import de.lolhens.http4s.spa._
import de.th3falc0n.mkts.Models.{Blocklist, IpEntry}
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

  private var dummyLists = Seq(
    Blocklist("test1"),
    Blocklist("test2"),
  )

  private val dummyEntries = Seq(
    IpEntry("a", true),
    IpEntry("b", true),
    IpEntry("c", true),
  )

  val toRoutes: HttpRoutes[IO] = {
    import org.http4s.dsl.io._
    Router(
      "/" -> appController.toRoutes,

      "/api" -> HttpRoutes.of {
        case GET -> Root / "lists" =>
          import org.http4s.circe.CirceEntityCodec._
          for {
            response <- Ok(dummyLists)
          } yield
            response

        case request@DELETE -> Root / "lists" =>
          import org.http4s.circe.CirceEntityCodec._
          for {
            list <- request.as[Blocklist]
            _ = dummyLists = dummyLists.filterNot(_ == list)
            response <- Ok(())
          } yield
            response

        case request@PUT -> Root / "lists" =>
          import org.http4s.circe.CirceEntityCodec._
          for {
            list <- request.as[Blocklist]
            _ = dummyLists = dummyLists :+ list
            response <- Ok(())
          } yield
            response

        case request@POST -> Root / "entries" =>
          import org.http4s.circe.CirceEntityCodec._
          for {
            list <- request.as[Blocklist]
            response <- Ok(dummyEntries :+ IpEntry(list.name, false))
          } yield
            response

        case request@POST -> Root / "entries" / "enabled" =>
          import org.http4s.circe.CirceEntityCodec._
          for {
            e <- request.as[(Blocklist, IpEntry, Boolean)]
            response <- Ok(())
          } yield
            response
      },
    )
  }
}
