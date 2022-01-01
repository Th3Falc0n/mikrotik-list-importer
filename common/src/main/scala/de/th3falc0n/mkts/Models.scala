package de.th3falc0n.mkts

import io.circe.generic.semiauto._
import io.circe.{ Codec, Decoder, Encoder }
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

object Models {
  case class AddressSourceName(string: String)

  object AddressSourceName {
    implicit val codec: Codec[AddressSourceName] = Codec.from(
      Decoder.decodeString.map(AddressSourceName.apply),
      Encoder.encodeString.contramap(_.string)
    )
  }

  case class AddressSource(name: AddressSourceName, enabled: Boolean = true)

  object AddressSource {
    implicit val codec: Codec[AddressSource] = deriveCodec
  }

  case class AddressListName(string: String)

  object AddressListName {
    implicit val codec: Codec[AddressListName] = Codec.from(
      Decoder.decodeString.map(AddressListName.apply),
      Encoder.encodeString.contramap(_.string)
    )
  }

  case class AddressList(name: AddressListName, sources: Seq[AddressSource], updateInterval: Duration)

  object AddressList {
    private implicit val durationCodec: Codec[Duration] = Codec.from(
      Decoder.decodeLong.map(Duration(_, TimeUnit.MILLISECONDS)),
      Encoder.encodeLong.contramap(_.toMillis)
    )
    implicit val codec: Codec[AddressList] = deriveCodec
  }

  case class IP(host: Int, maskBits: Int) {
    def isNetwork: Boolean = (host & (0xFFFFFFFF << (32 - maskBits))) == host

    def nextTwoSubs: Seq[IP] = {
      val subnet = host & (0xFFFFFFFF << (32 - maskBits))
      val a = subnet
      val b = a + (1 << (31 - maskBits))
      Seq(IP(a, maskBits + 1), IP(b, maskBits + 1))
    }

    def contains(other: IP): Boolean = {
      if (!this.isNetwork) throw new IllegalArgumentException("Not a network")

      if (other.maskBits < this.maskBits) return false

      (other.host & (0xFFFFFFFF << (32 - maskBits))) == host
    }

    override def toString: String = {
      val a = host >> 24 & 0xFF
      val b = host >> 16 & 0xFF
      val c = host >> 8 & 0xFF
      val d = host & 0xFF
      if (maskBits < 32) {
        s"$a.$b.$c.$d/$maskBits"
      } else {
        s"$a.$b.$c.$d"
      }
    }
  }

  object IP {
    def fromString(ipString: String): IP = {
      val parts = ipString.split('/')
      val host = parts(0)
      val netmask = if (parts.length == 2) parts(1) else "32"

      IP(host.split('.').map(_.toInt).reduceLeft(_ * 256 + _), netmask.toInt)
    }

    implicit val codec: Codec[IP] = deriveCodec
  }

}
