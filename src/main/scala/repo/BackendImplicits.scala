package de.th3falc0n.mkts
package repo

import Models.{ AddressList, AddressSource, IP }
import backend.MikrotikConnection
import cache.Cache
import ip.IPMerger

import cats.effect.{ ExitCode, IO }
import cats.implicits.toTraverseOps
import org.http4s.Uri
import org.slf4j.LoggerFactory

import java.time.Duration
import org.http4s.client.blaze._

import scala.concurrent.ExecutionContext.Implicits.global

object BackendImplicits {
  implicit class BackendAddressSource(addressSource: AddressSource) {
    private val uri = Uri.fromString(addressSource.name).toOption.get
    private val logger = LoggerFactory.getLogger(s"iplist-${addressSource.name}")

    def fetch: IO[Seq[IP]] = {
      logger.info("Fetching")

      val ips = Cache.getOrElseUpdate(
        s"fetch-${addressSource.hashCode()}",
        BlazeClientBuilder[IO].resource.use { client =>
          val request = client.expect[String](uri)

          request.map { r =>
            val result = r
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

            logger.info("Got {} IPs", result.length, addressSource.name)

            result.toSeq
          }
        }
          .handleErrorWith { error =>
            logger.error("Error while fetching", error)
            IO.pure(Seq.empty)
          }
      )

      ips
    }
  }

  implicit class BackendAddressList(addressList: AddressList) {
    private val logger = LoggerFactory.getLogger("AddressList-" + addressList.name)

    def fetch: IO[Seq[IP]] = {
      for {
        ips <- addressList.sources.map(_.fetch).sequence.map(_.flatten)
      } yield {
        logger.info("Got {} unique IPs", ips.length)
        val merged = IPMerger.mergeIPs(ips)

        logger.info("Reduced to {} unique list entries", merged.length)
        merged
      }
    }

    def update: IO[ExitCode] = {
      for {
        merged <- fetch
      } yield {
        MikrotikConnection.updateList(addressList.name, merged)

        logger.info("Updated list {} with {} unique IPs", addressList.name, merged.length)
        ExitCode.Success
      }
    }
  }
}
