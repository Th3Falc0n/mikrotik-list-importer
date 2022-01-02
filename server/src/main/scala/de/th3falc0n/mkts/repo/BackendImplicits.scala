package de.th3falc0n.mkts.repo

import cats.effect.{ExitCode, IO}
import de.th3falc0n.mkts.Models.{AddressList, AddressSource, IP, IPListEntry}
import de.th3falc0n.mkts.backend.MikrotikConnection
import de.th3falc0n.mkts.cache.Cache
import de.th3falc0n.mkts.ip.IPMerger
import org.slf4j.LoggerFactory
import sttp.client3.{HttpURLConnectionBackend, basicRequest}
import sttp.model.Uri

import java.net.URI
import java.time.Duration

object BackendImplicits {
  implicit class BackendAddressSource(addressSource: AddressSource) {
    private val uri = Uri(URI.create(addressSource.name.string))
    private val logger = LoggerFactory.getLogger(s"iplist-${addressSource.name.string}")

    private val EntryPattern = "^([0-9][0-9./]*)(?:\\s*;(.*))?.*".r

    def fetch: Seq[IPListEntry] = {
      logger.info("Getting")

      val response = Cache.getOrElseUpdate(
        s"fetch-${addressSource.hashCode()}",
        Duration.ofMinutes(15),
        {
          logger.info("Fetching")
          val request = basicRequest.get(uri)
          val backend = HttpURLConnectionBackend()
          request.send(backend).body
        }
      )

      val ips = response
        .getOrElse("")
        .split("\n")
        .filter(_.nonEmpty)
        .collect {
          case EntryPattern(ipString, comment) =>
            IPListEntry(IP.fromString(ipString), Option(comment).map(_.trim).getOrElse(""))
        }
        .filter {
          case entry if IP.fromString("10.0.0.0/8").contains(entry.ip) => false
          case entry if IP.fromString("172.16.0.0/12").contains(entry.ip) => false
          case entry if IP.fromString("192.168.0.0/16").contains(entry.ip) => false
          case _ => true
        }

      logger.info("Got {} IPs", ips.length, addressSource.name.string)
      ips
    }
  }

  implicit class BackendAddressList(addressList: AddressList) {
    private val logger = LoggerFactory.getLogger("AddressList-" + addressList.name.string)

    def fetch: Seq[IPListEntry] = {
      logger.info("Updating list {} with {} sources", addressList.name.string, addressList.sources.length)
      val ips = addressList.sources.flatMap(_.fetch)

      logger.info("Got {} unique IPs", ips.length)
      val merged = IPMerger.mergeIPs(ips.map(_.ip))

      logger.info("Reduced to {} unique list entries", merged.length)
      merged.map(IPListEntry(_, ""))
    }

    def update: IO[ExitCode] = IO {
      try {
        val merged = fetch

        MikrotikConnection.updateList(addressList.name.string, merged.map(_.ip))

        logger.info("Updated list {} with {} unique IPs", addressList.name.string, merged.length)
        ExitCode.Success
      }
      catch {
        case e: Exception => {
          logger.error("Error updating list {}", addressList.name.string, e)
          ExitCode.Error
        }
      }
    }
  }
}
