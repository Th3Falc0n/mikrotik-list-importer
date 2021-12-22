package de.th3falc0n.mkts.lists

import de.th3falc0n.mkts.ip.IP

trait IPSource {
  val name: String

  def fetch: Seq[IP]
}
