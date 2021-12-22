package de.th3falc0n.mkts

import io.circe.Codec
import io.circe.generic.semiauto._

object Models {
  case class IpEntry(string: String)

  object IpEntry {
    implicit val codec: Codec[IpEntry] = deriveCodec
  }
}
