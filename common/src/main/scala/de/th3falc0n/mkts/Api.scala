package de.th3falc0n.mkts

import cats.effect.IO
import de.lolhens.remoteio.Rest.RestClientImpl
import de.lolhens.remoteio.Rpc.RpcClientImpl
import de.lolhens.remoteio.{Rest, Rpc}
import de.th3falc0n.mkts.Models.{AddressList, AddressListName, AddressSource, AddressSourceName, IP, IPListEntry}
import org.http4s.Method._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.implicits._
import org.http4s.{Method, Request}

object Api {
  val lists = Rpc[IO, Unit, Seq[AddressList]](Rest)(GET -> path"lists")
  val deleteList = Rpc[IO, AddressListName, Unit](Rest)(DELETE -> path"lists")
  val putList = Rpc[IO, AddressList, Unit](Rest)(PUT -> path"lists")

  def deleteSource(addressListName: AddressListName,
                   addressSourceName: AddressSourceName)
                  (implicit clientImpl: RestClientImpl[IO]): IO[Unit] =
    for {
      addressListOption <- lists(()).map(_.find(_.name == addressListName))
      _ <- addressListOption match {
        case Some(addressList) =>
          putList(addressList.copy(sources = addressList.sources.filterNot(_.name == addressSourceName)))

        case None => IO.unit
      }
    } yield ()

  def putSource(addressListName: AddressListName,
                addressSource: AddressSource)
               (implicit clientImpl: RestClientImpl[IO]): IO[Unit] =
    for {
      addressListOption <- lists(()).map(_.find(_.name == addressListName))
      _ <- addressListOption match {
        case Some(addressList) =>
          putList(addressList.copy(sources = addressList.sources :+ addressSource))

        case None => IO.unit
      }
    } yield ()

  // TODO implement path codecs
  val listEntries = Rpc[IO, AddressListName, Seq[IPListEntry]](Rest)(GET -> path"lists/todo/entries")
  val sourceEntries = Rpc[IO, (AddressListName, AddressSourceName), Seq[IPListEntry]](Rest)(
    GET -> path"lists/todo/sources/todo/entries")

  def entries(addressListName: AddressListName,
              addressSourceName: Option[AddressSourceName])
             (implicit clientImpl: RestClientImpl[IO]): IO[Seq[IPListEntry]] =
    addressSourceName match {
      case Some(sourceName) =>
        //uri"/api/lists" / addressListName.string / "sources" / sourceName.string / "entries"
      sourceEntries((addressListName, sourceName))

      case None =>
        //uri"/api/lists" / addressListName.string / "entries"
      listEntries(addressListName)
    }

  val updateEntry = Rpc[IO, (AddressListName, IP, Boolean), Unit](Rest)(POST -> path"entries/enabled")
}
