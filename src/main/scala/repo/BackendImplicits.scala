package de.th3falc0n.mkts
package repo

import Models.{ AddressList, AddressSource, IP }
import backend.MikrotikConnection
import cache.Cache
import ip.IPMerger

import cats.effect.{ ExitCode, IO }
import org.slf4j.LoggerFactory
import sttp.client3.{ HttpURLConnectionBackend, basicRequest }
import sttp.model.Uri

import java.net.URI
import java.time.Duration

object BackendImplicits  {
  implicit class BackendAddressSource(addressSource: AddressSource) {
    private val uri = Uri(URI.create(addressSource.name))
    private val logger = LoggerFactory.getLogger(s"iplist-${addressSource.name}")

    def fetch: Seq[IP] = {
      logger.info("Fetching")

      val response = Cache.getOrElseUpdate(
        s"fetch-${addressSource.hashCode()}",
        Duration.ofMinutes(15),
        {
          val request = basicRequest.get(uri)
          val backend = HttpURLConnectionBackend()
          request.send(backend).body
        }
      )

      val ips = response
        .getOrElse("")
        .split("\n")
        .filter(_.nonEmpty)
        .map(_.split(' ').head)
        .filter(ip => "^[0-9][0-9./]*$".r.matches(ip))
        .map(IP.fromString)
        .filter {
          case ip if IP.fromString("10.0.0.0/8").contains(ip) => false
          case ip if IP.fromString("172.16.0.0/12").contains(ip) => false
          case ip if IP.fromString("192.168.0.0/16").contains(ip) => false
          case _ => true
        }

      logger.info("Got {} IPs", ips.length, addressSource.name)
      ips
    }
  }

  implicit class BackendAddressList(addressList: AddressList) {
    private val logger = LoggerFactory.getLogger("AddressList-" + addressList.name)

    def fetch: Seq[IP] = {
      logger.info("Updating list {} with {} sources", addressList.name, addressList.sources.length)
      val ips = addressList.sources.flatMap(_.fetch)

      logger.info("Got {} unique IPs", ips.length)
      val merged = IPMerger.mergeIPs(ips)

      logger.info("Reduced to {} unique list entries", merged.length)
      merged
    }

    def update: IO[ExitCode] = IO {
      try {
        val merged = fetch

        MikrotikConnection.updateList(addressList.name, merged)

        logger.info("Updated list {} with {} unique IPs", addressList.name, merged.length)
        ExitCode.Success
      }
      catch {
        case e: Exception =>
          logger.error("Error updating list {}", addressList.name, e)
          ExitCode.Error
      }
    }
  }
}
