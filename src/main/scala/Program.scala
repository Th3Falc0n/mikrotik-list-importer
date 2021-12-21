package de.th3falc0n.mkts

import com.typesafe.config.ConfigFactory
import me.legrange.mikrotik.ApiConnection
import org.slf4j.{ Logger, LoggerFactory }
import sttp.client3._
import sttp.model.Uri

import java.net.URI
import scala.collection.convert.ImplicitConversions.`map AsScala`
import scala.collection.JavaConverters._

object Program extends App {
  val config = ConfigFactory.load()
  val logger = LoggerFactory.getLogger(this.getClass)

  logger.info("Starting MKTS")
  logger.info("Connecting to API at {}", config.getString("mikrotik.host"))
  val api = ApiConnection.connect(config.getString("mikrotik.host"))

  logger.info("Logging in with user {}", config.getString("mikrotik.user"))
  api.login(config.getString("mikrotik.user"), config.getString("mikrotik.password"))

  def removeList(name: String) = {
    val result = api.execute(s"/ip/firewall/address-list/print where list=$name return .id").asScala

    if(result.length > 0) {
      val ids = result.map(_.apply(".id")).mkString(",")
      api.execute(s"/ip/firewall/address-list/remove numbers=$ids")
    }
  }

  def updateList(sources: Seq[Uri], name: String) = {
    logger.info("Updating list {} with {} sources", name, sources.length)

    val ips = sources.flatMap(src => {
      logger.info("Fetching list from {}", src)
      val request = basicRequest.get(src)

      val backend = HttpURLConnectionBackend()
      val response = request.send(backend)

      val ips = response
        .body.getOrElse("")
        .split("\n")
        .filter(_.nonEmpty)
        .filter(k => "0123456789".contains(k.head))
        .map(_.split(' ').head)

      logger.info("Got {} IPs from {}", ips.length, src)
      ips
    })

    def range(i: Int, min: Int, max: Int) = i <= max && i >= min

    val usableIPs = ips.map {
      case ip if ip.startsWith("10.") => "#"
      case ip if ip.startsWith("172.") && range(ip.split('.').apply(1).toInt, 16, 31) => "#"
      case ip if ip.startsWith("192.168.") => "#"
      case ip => ip
    }.filterNot(_.contains("#")).distinct

    logger.info("Clearing list {}", name)
    removeList(name)

    logger.info("Adding {} IPs to list {}", usableIPs.length, name)
    val commands = usableIPs.map(ip => s"/ip/firewall/address-list/add list=$name address=\"$ip\"")
    commands.foreach(api.execute)
  }

  val lists = config.getConfigList("lists")

  lists.forEach(list => {
    val name = list.getString("name")
    val sources = list.getStringList("sources").asScala.toSeq.map(uriString => Uri(URI.create(uriString)))
    updateList(sources, name)
  })

  logger.info("Finished successfully!")

  api.close()
}
