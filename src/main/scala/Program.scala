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

  def updateList(sources: Seq[Uri], name: String) = {
    logger.info("Updating list {} with {} sources", name, sources.length)

    val netsFromFile = sources.flatMap(src => {
      def range(i: Int, min: Int, max: Int) = i <= max && i >= min

      logger.info("Fetching list from {}", src)
      val request = basicRequest.get(src)

      val backend = HttpURLConnectionBackend()
      val response = request.send(backend)

      val ips = response
        .body.getOrElse("")
        .split("\n")
        .filter(_.nonEmpty)
        .map(_.split(' ').head)
        .filter(ip => "^[0-9][0123456789.\\/]*$".r.matches(ip))
        .map {
          case ip if ip.startsWith("10.") => "#"
          case ip if ip.startsWith("172.") && range(ip.split('.').apply(1).toInt, 16, 31) => "#"
          case ip if ip.startsWith("192.168.") => "#"
          case ip => ip
        }

      logger.info("Got {} IPs from {}", ips.length, src)
      ips
    }).distinct

    logger.info("Got {} unique IPs", netsFromFile.length)

    val listIPs = netsFromFile

    val result = api.execute(s"/ip/firewall/address-list/print where list=$name return address, .id").asScala.toSeq
    val usedIPs = result.map(_.get("address"))

    val toAdd = listIPs.diff(usedIPs)
    val toRemove = usedIPs.diff(listIPs)

    logger.info("Removing {} IPs from list {}", toRemove.length, name)

    val toRemoveIds = toRemove.map(tr => result.find(_.get("address") == tr).get.get(".id"))

    if(toRemoveIds.nonEmpty) {
      api.execute(s"/ip/firewall/address-list/remove numbers=${toRemoveIds.mkString(",")}")
    }

    logger.info("Adding {} IPs to list {}", toAdd.length, name)
    toAdd.foreach(ip => api.execute(s"/ip/firewall/address-list/add list=$name address=\"$ip\""))
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
