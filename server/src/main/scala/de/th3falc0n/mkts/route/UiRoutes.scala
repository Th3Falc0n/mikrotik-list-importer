package de.th3falc0n.mkts.route

import cats.data.Kleisli
import cats.effect.IO
import cats.syntax.option._
import de.lolhens.http4s.spa._
import de.th3falc0n.mkts.Models.{AddressList, AddressListName, IpEntry}
import de.th3falc0n.mkts.repo.AddressListRepo
import org.http4s.server.Router
import org.http4s.server.middleware.GZip
import org.http4s.server.staticcontent.ResourceServiceBuilder
import org.http4s.{HttpRoutes, Uri}

import scala.util.chaining._

class UiRoutes(addressListRepo: AddressListRepo[IO]) {
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
      "/" -> appController.toRoutes.pipe(GZip(_)),

      "/api" -> HttpRoutes.of {
        case GET -> Root / "lists" =>
          import org.http4s.circe.CirceEntityCodec._
          for {
            lists <- addressListRepo.list
            response <- Ok(lists)
          } yield
            response

        case request@DELETE -> Root / "lists" =>
          import org.http4s.circe.CirceEntityCodec._
          for {
            listName <- request.as[AddressListName]
            _ <- addressListRepo.delete(listName)
            response <- Ok(())
          } yield
            response

        case request@PUT -> Root / "lists" =>
          import org.http4s.circe.CirceEntityCodec._
          for {
            list <- request.as[AddressList]
            _ <- addressListRepo.put(list)
            response <- Ok(())
          } yield
            response

        case request@GET -> Root / "lists" / addressListNameString / "entries" =>
          val addressListName = AddressListName(addressListNameString)
          import org.http4s.circe.CirceEntityCodec._
          for {
            //entries <- request.as[(AddressListName, Option[AddressSource])]
            response <- Ok(Seq.empty[IpEntry])
          } yield
            response

        case request@POST -> Root / "entries" / "enabled" =>
          import org.http4s.circe.CirceEntityCodec._
          for {
            //e <- request.as[(Blocklist, IpEntry, Boolean)]
            response <- Ok(())
          } yield
            response
      },
    )
  }
}
