package de.th3falc0n.mkts.backend

import de.th3falc0n.mkts.Main.config
import de.th3falc0n.mkts.ip.IP
import me.legrange.mikrotik.ApiConnection
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters._

object MikrotikConnection {
  val logger = LoggerFactory.getLogger(this.getClass)

  logger.info("Connecting to API at {}", config.getString("mikrotik.host"))
  val api = ApiConnection.connect(config.getString("mikrotik.host"))

  logger.info("Logging in with user {}", config.getString("mikrotik.user"))
  api.login(config.getString("mikrotik.user"), config.getString("mikrotik.password"))

  def updateList(name: String, ips: Seq[IP]) = {
    logger.info("Updating list {}", name)
    val ipsAsString = ips.map(_.toString)

    val result = api.execute(s"/ip/firewall/address-list/print where list=$name return address, .id").asScala.toSeq
    val usedIPs = result.map(_.get("address"))

    val toAdd = ipsAsString.diff(usedIPs)
    val toRemove = usedIPs.diff(ipsAsString)

    logger.info("Removing {} IPs from list {}", toRemove.length, name)

    val toRemoveIds = toRemove.map(tr => result.find(_.get("address") == tr).get.get(".id"))

    if(toRemoveIds.nonEmpty) {
      api.execute(s"/ip/firewall/address-list/remove numbers=${toRemoveIds.mkString(",")}")
    }

    logger.info("Adding {} IPs to list {}", toAdd.length, name)
    toAdd.foreach(ip => api.execute(s"/ip/firewall/address-list/add list=$name address=\"$ip\""))
  }
}
