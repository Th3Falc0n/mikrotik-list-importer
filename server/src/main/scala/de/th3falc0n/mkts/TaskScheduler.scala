package de.th3falc0n.mkts

import de.th3falc0n.mkts.Main.config
import de.th3falc0n.mkts.backend.MikrotikConnection
import de.th3falc0n.mkts.ip.IPMerger
import de.th3falc0n.mkts.lists.{HttpIPSource, IPSource}
import org.slf4j.LoggerFactory

import java.util.concurrent.Executors
import scala.jdk.CollectionConverters._

class IPUpdateListTask(val name: String, val sources: Seq[IPSource]) {
  private val logger = LoggerFactory.getLogger("IPUpdateListTask-" + name)

  def update = {
    logger.info("Updating list {} with {} sources", name, sources.length)
    val ips = sources.flatMap(_.fetch)

    logger.info("Got {} unique IPs", ips.length)
    val merged = IPMerger.mergeIPs(ips)

    logger.info("Reduced to {} unique list entries", merged.length)
    MikrotikConnection.updateList(name, merged)
  }
}

object TaskScheduler {
  private val logger = LoggerFactory.getLogger(this.getClass)
  val scheduler = Executors.newScheduledThreadPool(1)

  def initialize = {

    val lists = config.getConfigList("lists")

    lists.forEach(list => {
      val name = list.getString("name")
      val sources = list.getStringList("sources").asScala.toSeq.map(new HttpIPSource(_))
      val interval = list.getDuration("update-interval")

      logger.info("Schedule list update: " + name + " every " + interval.toString)

      val updateTask = new IPUpdateListTask(name, sources)

      scheduler.scheduleAtFixedRate(() => updateTask.update, 0, interval.toMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
    })
  }
}
