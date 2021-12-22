package de.th3falc0n.mkts

import io.circe.generic.semiauto._
import io.circe.{Codec, Decoder, Encoder}

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

  case class IpEntry(string: String)

  object IpEntry {
    implicit val codec: Codec[IpEntry] = deriveCodec
  }
}
