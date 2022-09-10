package de.th3falc0n.mkts

import Models.{ AddressList, AddressSource }
import repo.AddressListRepo
import repo.BackendImplicits.BackendAddressList

import cats.effect.unsafe.implicits.global
import cats.effect.{ ExitCode, IO, IOApp }
import cats.implicits._
import com.typesafe.config.{ Config, ConfigFactory }
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.jdk.CollectionConverters.CollectionHasAsScala

object Main extends IOApp {
  private val logger = LoggerFactory.getLogger(this.getClass)
  logger.info("Initializing")

  val config: Config = ConfigFactory.load()

  val activeRepo: AddressListRepo[IO] = AddressListRepo.inMemImpl

  override def run(args: List[String]): IO[ExitCode] = {
    logger.info("Starting server and background tasks")

    config.getConfigList("lists").forEach(list => {
      val addressList = AddressList(
        list.getString("name"),
        list.getStringList("sources").asScala.toSeq.map(src => AddressSource(src)),
        Duration.apply(list.getDuration("update-interval").toMillis, TimeUnit.MILLISECONDS)
      )
      logger.info("Loaded address list {} from application config", addressList)
      activeRepo.put(addressList).unsafeRunSync()
    })

    activeRepo.list.flatMap { lists =>
      logger.info(s"Found ${lists.size} lists")

      lists.map { list =>
        logger.info("Started for {}", list.toString)

        def update: IO[Unit] = list.update >> IO.sleep(list.updateInterval.asInstanceOf[FiniteDuration]) >> IO.defer(update)

        update
      }.sequence
    }.start.flatMap(_ => IO.never).as(ExitCode.Success)
  }
}
