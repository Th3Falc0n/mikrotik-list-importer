package de.th3falc0n.mkts.route

import cats.data.Kleisli
import cats.effect.IO
import cats.syntax.option._
import de.lolhens.http4s.spa._
import de.th3falc0n.mkts.Models.{ AddressList, AddressListName, AddressSource, AddressSourceName }
import de.th3falc0n.mkts.repo.AddressListRepo
import de.th3falc0n.mkts.repo.BackendImplicits.{ BackendAddressList, BackendAddressSource }
import org.http4s.server.Router
import org.http4s.server.middleware.GZip
import org.http4s.server.staticcontent.ResourceServiceBuilder
import org.http4s.{ HttpRoutes, Uri }
import org.slf4j.LoggerFactory

import scala.util.chaining._

class UiRoutes(addressListRepo: AddressListRepo[IO]) {
  private val logger = LoggerFactory.getLogger("UiRoutes")

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
          logger.info("GET /api/lists")
          import org.http4s.circe.CirceEntityCodec._
          for {
            lists <- addressListRepo.list
            response <- Ok(lists)
          } yield
            response

        case request@DELETE -> Root / "lists" / addressListNameString =>
          logger.info(s"DELETE /api/lists/$addressListNameString")
          val addressListName = AddressListName(addressListNameString)
          import org.http4s.circe.CirceEntityCodec._
          for {
            _ <- addressListRepo.delete(addressListName)
            response <- Ok(())
          } yield
            response

        case request@PUT -> Root / "lists" =>
          logger.info(s"PUT /api/lists")
          import org.http4s.circe.CirceEntityCodec._
          for {
            list <- request.as[AddressList]
            _ <- addressListRepo.put(list)
            response <- Ok(())
          } yield
            response

        case request@GET -> Root / "lists" / addressListNameString / "entries" =>
          logger.info(s"GET /api/lists/$addressListNameString/entries")
          val addressListName = AddressListName(addressListNameString)
          import org.http4s.circe.CirceEntityCodec._
          import de.th3falc0n.mkts.Models.IP._
          for {
            list <- addressListRepo.list.map { list =>
              list.find(_.name == addressListName)
            }
            response <- Ok(list.map(_.fetch).getOrElse(Seq.empty))
          } yield
            response

        case request@GET -> Root / "lists" / addressListNameString / "sources" =>
          logger.info(s"GET /api/lists/$addressListNameString/sources")
          val addressListName = AddressListName(addressListNameString)
          import org.http4s.circe.CirceEntityCodec._
          for {
            list <- addressListRepo.list.map(_.find(_.name == addressListName))
            response <- Ok(list.map(_.sources).getOrElse(Seq.empty))
          } yield
            response

        case request@GET -> Root / "lists" / addressListNameString / "sources" / addressSourceNameString / "entries" =>
          logger.info(s"GET /api/lists/$addressListNameString/sources/$addressSourceNameString/entries")
          val addressListName = AddressListName(addressListNameString)
          val addressSourceName = AddressSourceName(addressSourceNameString)
          import org.http4s.circe.CirceEntityCodec._
          for {
            list <- addressListRepo.list.map(_.find(_.name == addressListName))
            source = list.flatMap(_.sources.find(_.name == addressSourceName))
            response <- Ok(source.map(_.fetch).getOrElse(Seq.empty))
          } yield
            response

        /*case request@POST -> Root / "entries" / "enabled" =>
          import org.http4s.circe.CirceEntityCodec._
          for {
            //e <- request.as[(Blocklist, IpEntry, Boolean)]
            response <- Ok(())
          } yield
            response*/
      }
    )
  }
}
