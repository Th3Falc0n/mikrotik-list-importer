package de.th3falc0n.mkts

import Program.logger

import org.slf4j.LoggerFactory

case class IP(host: Int, maskBits: Int) {
  def isNetwork = (host & (0xFFFFFFFF << (32 - maskBits))) == host

  def nextTwoSubs = {
    val subnet = host & (0xFFFFFFFF << (32 - maskBits))
    val a = subnet
    val b = a + (1 << (31 - maskBits))
    Seq(IP(a, maskBits + 1), IP(b, maskBits + 1))
  }

  def contains(other: IP): Boolean = {
    if(!this.isNetwork) throw new IllegalArgumentException("Not a network")

    if(other.maskBits < this.maskBits) return false

    (other.host & (0xFFFFFFFF << (32 - maskBits))) == host
  }

  override def toString: String = {
    val a = host >> 24 & 0xFF
    val b = host >> 16 & 0xFF
    val c = host >> 8 & 0xFF
    val d = host & 0xFF
    if(maskBits < 32) {
      s"$a.$b.$c.$d/${maskBits}"
    } else {
      s"$a.$b.$c.$d"
    }
  }
}

object IPMerger {
  val logger = LoggerFactory.getLogger("IPMerger")

  def fromStrings(raw: Seq[String]): Seq[IP] = {
    raw.map(raw => {
      val parts = raw.split('/')
      val host = parts(0)
      val netmask = if(parts.length == 2) parts(1) else "32"

      IP(host.split('.').map(_.toInt).reduceLeft(_ * 256 + _), netmask.toInt)
    })
  }

  def mergeIPs(in: Seq[IP], depth: Int = 31): Seq[IP] = {
    val distinct = in.distinct
    val ips = distinct.filterNot(a => distinct.filter(_ != a).exists(b => b.contains(a)))
    logger.debug(ips.mkString(","))

    val atDepth = ips.filter(ip => ip.maskBits == depth + 1)
    logger.debug("Found {} usable IPs at depth {}", atDepth.length, depth)

    val passed = ips.diff(atDepth)
    logger.debug("Ignored {} IPs at depth {}", passed.length, depth)

    val possibleNetworks = atDepth.map(i => IP(i.host, depth)).filter(_.isNetwork)
    logger.debug("{} possible merges at depth {}", possibleNetworks.length, depth)

    val merged = possibleNetworks.filter(_.nextTwoSubs.forall(atDepth.contains(_)))
    logger.debug("{} actual merges at depth {}", merged.length, depth)

    val mergedNets = merged.flatMap(_.nextTwoSubs)
    val notMerged = atDepth.diff(mergedNets)
    logger.debug(" {} not merged at depth {}", notMerged.length, depth)

    val continueToMerge = passed ++ merged

    if(continueToMerge.nonEmpty) {
      notMerged ++ mergeIPs(continueToMerge, depth - 1)
    } else {
      notMerged
    }
  }
}
