package de.th3falc0n.mkts.ip

import org.slf4j.LoggerFactory

object IPMerger {
  val logger = LoggerFactory.getLogger("IPMerger")

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
