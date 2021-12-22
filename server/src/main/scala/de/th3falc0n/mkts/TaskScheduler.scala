package de.th3falc0n.mkts

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import de.th3falc0n.mkts.Main.config
import org.slf4j.LoggerFactory

import java.util.concurrent.{ Executors, TimeUnit }
import scala.jdk.CollectionConverters._

object TaskScheduler {
  private val logger = LoggerFactory.getLogger("TaskScheduler")
  private val scheduler = Executors.newScheduledThreadPool(1)

  def start(): Unit = {
    import de.th3falc0n.mkts.repo.BackendImplicits._
    logger.info("Starting task scheduler")

    Main.activeRepo.list.map { lists =>
      logger.info(s"Found ${lists.size} lists")
      lists.map { list =>
        logger.info("Started for {}", list.toString)
        scheduler.scheduleAtFixedRate(
          () => list.update.unsafeRunSync(),
          0,
          list.updateInterval.toMillis,
          TimeUnit.MILLISECONDS
        )
      }
    }.unsafeRunSync()
  }
}
