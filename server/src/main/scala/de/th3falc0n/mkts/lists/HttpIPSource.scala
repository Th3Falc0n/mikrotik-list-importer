package de.th3falc0n.mkts.lists

import de.th3falc0n.mkts.ip.IP
import org.slf4j.LoggerFactory
import sttp.client3.{ HttpURLConnectionBackend, basicRequest }
import sttp.model.Uri

import java.net.URI

class HttpIPSource(val src: String) extends IPSource {
  private val uri = Uri(URI.create(src))
  private val logger = LoggerFactory.getLogger(s"iplist-$src")

  def fetch: Seq[IP] = {
    logger.info("Fetching", src)
    val request = basicRequest.get(uri)

    val backend = HttpURLConnectionBackend()
    val response = request.send(backend)

    val ips = response
      .body.getOrElse("")
      .split("\n")
      .filter(_.nonEmpty)
      .map(_.split(' ').head)
      .filter(ip => "^[0-9][0123456789./]*$".r.matches(ip))
      .map(IP.fromString)
      .filter {
        case ip if IP.fromString("10.0.0.0/8").contains(ip) => false
        case ip if IP.fromString("172.16.0.0/12").contains(ip) => false
        case ip if IP.fromString("192.168.0.0/16").contains(ip) => false
        case _ => true
      }

    logger.info("Got {} IPs", ips.length, src)
    ips
  }
}
