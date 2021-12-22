package de.th3falc0n.mkts.lists

import de.th3falc0n.mkts.ip.IP
import org.slf4j.LoggerFactory
import sttp.client3.{ HttpURLConnectionBackend, basicRequest }
import sttp.model.Uri

import java.net.URI

class HttpIPSource(val src: String) extends IPSource {
  private val uri = Uri(URI.create(src))
  private val logger = LoggerFactory.getLogger(s"iplist-$src")

  private def range(i: Int, min: Int, max: Int) = i <= max && i >= min

  def fetch = {
    logger.info("Fetching", src)
    val request = basicRequest.get(uri)

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

    logger.info("Got {} IPs", ips.length, src)
    ips.map(IP.fromString)
  }
}
