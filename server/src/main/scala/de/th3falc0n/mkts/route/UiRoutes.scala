package de.th3falc0n.mkts.route

import cats.data.Kleisli
import cats.effect.IO
import cats.syntax.option._
import de.lolhens.http4s.spa._
import de.lolhens.remoteio.Rest
import de.th3falc0n.mkts.Api
import de.th3falc0n.mkts.repo.AddressListRepo
import de.th3falc0n.mkts.repo.BackendImplicits.{BackendAddressList, BackendAddressSource}
import org.http4s.server.Router
import org.http4s.server.middleware.GZip
import org.http4s.server.staticcontent.ResourceServiceBuilder
import org.http4s.{HttpRoutes, Uri}
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
      SpaDependencies.mainCss,
    ),
  )

  private val appController = SinglePageAppController[IO](
    mountPoint = Uri.Root,
    controller = Kleisli.pure(app),
    resourceServiceBuilder = ResourceServiceBuilder[IO]("/assets").some
  )

  private val apiRoutes: HttpRoutes[IO] = Rest.toRoutes(
    Api.lists.impl { _ =>
      logger.info("GET /api/lists")
      addressListRepo.list
    },
    Api.deleteList.impl { addressListName =>
      logger.info(s"DELETE /api/lists/${addressListName.string}")
      addressListRepo.delete(addressListName)
    },
    Api.putList.impl { addressList =>
      logger.info(s"PUT /api/lists")
      addressListRepo.put(addressList)
    },
    Api.listEntries.impl { addressListName =>
      logger.info(s"GET /api/lists/${addressListName.string}/entries")
      addressListRepo.list.map { list =>
        list.find(_.name == addressListName)
      }.map(_.map(_.fetch).getOrElse(Seq.empty))
    },
    Api.sourceEntries.impl { case (addressListName, addressSourceName) =>
      logger.info(s"GET /api/lists/${addressListName.string}/sources/${addressSourceName.string}/entries")
      for {
        list <- addressListRepo.list.map(_.find(_.name == addressListName))
        source = list.flatMap(_.sources.find(_.name == addressSourceName))
      } yield
        source.map(_.fetch).getOrElse(Seq.empty)
    },
  )

  val toRoutes: HttpRoutes[IO] = {
    Router(
      "/" -> appController.toRoutes,
      "/api" -> apiRoutes
    ).pipe(GZip(_))
  }
}
