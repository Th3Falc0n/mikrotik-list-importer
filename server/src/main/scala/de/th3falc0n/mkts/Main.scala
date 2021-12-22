package de.th3falc0n.mkts

import cats.effect.unsafe.implicits.global
import cats.effect.{ ExitCode, IO, IOApp }
import com.typesafe.config.ConfigFactory
import de.th3falc0n.mkts.Models.{ AddressList, AddressListName, AddressSource, AddressSourceName }
import de.th3falc0n.mkts.repo.AddressListRepo
import de.th3falc0n.mkts.route.UiRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters.CollectionHasAsScala

object Main extends IOApp {
  private val logger = LoggerFactory.getLogger(this.getClass)
  logger.info("Initializing")

  val config = ConfigFactory.load()

  val activeRepo = AddressListRepo.inMemImpl

  override def run(args: List[String]): IO[ExitCode] = {
    logger.info("Starting server and background tasks")

    config.getConfigList("lists").forEach(list => {
      val addressList = AddressList(
        AddressListName(list.getString("name")),
        list.getStringList("sources").asScala.toSeq.map(src => AddressSource(AddressSourceName(src))),
        Duration.apply(list.getDuration("update-interval").toMillis, TimeUnit.MILLISECONDS)
      )
      logger.info("Loaded address list {} from application config", addressList)
      activeRepo.put(addressList).unsafeRunSync()
    })

    TaskScheduler.start()

    BlazeServerBuilder[IO]
      .bindHttp(8080, "0.0.0.0")
      .withHttpApp(new UiRoutes(activeRepo).toRoutes.orNotFound)
      .resource
      .use(_ => IO.never)
  }
}
