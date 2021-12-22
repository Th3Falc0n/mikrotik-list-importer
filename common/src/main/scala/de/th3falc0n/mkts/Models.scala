package de.th3falc0n.mkts

import io.circe.Codec
import io.circe.generic.semiauto._

object Models {
  case class Blocklist(name: String)

  object Blocklist {
    implicit val codec: Codec[Blocklist] = deriveCodec
  }

  case class IpEntry(string: String, enabled: Boolean)

  object IpEntry {
    implicit val codec: Codec[IpEntry] = deriveCodec
  }
}
