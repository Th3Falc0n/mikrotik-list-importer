package de.th3falc0n.mkts

import cats.effect.unsafe.Scheduler
import cats.effect.{ ExitCode, IO, IOApp }
import com.typesafe.config.ConfigFactory
import org.http4s.blaze.server.BlazeServerBuilder
import org.slf4j.LoggerFactory

import java.util.concurrent.{ Executors, ScheduledExecutorService }

object Main extends IOApp {
  private val logger = LoggerFactory.getLogger(this.getClass)
  logger.info("Initializing")

  val config = ConfigFactory.load()

  override def run(args: List[String]): IO[ExitCode] = {
    logger.info("Starting server and background tasks")

    TaskScheduler.initialize

    BlazeServerBuilder[IO]
      .bindHttp(8080, "0.0.0.0")
      .withHttpApp(new UiRoutes().toRoutes.orNotFound)
      .resource
      .use(_ => IO.never)
  }
}
