package de.th3falc0n.mkts

import cats.effect.IO
import de.th3falc0n.mkts.Models.{AddressList, AddressListName, AddressSourceName, IpEntry}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dom.FetchClientBuilder
import org.http4s.implicits._
import org.http4s.{Method, Request}

object Api {
  private lazy val client = FetchClientBuilder[IO].create

  def lists: IO[Seq[AddressList]] =
    client.expect[Seq[AddressList]](Request[IO](method = Method.GET, uri = uri"/api/lists"))

  def deleteList(list: AddressListName): IO[Unit] =
    client.expect[Unit](Request[IO](method = Method.DELETE, uri = uri"/api/lists").withEntity(list))

  def putList(list: AddressList): IO[Unit] =
    client.expect[Unit](Request[IO](method = Method.PUT, uri = uri"/api/lists").withEntity(list))

  def entries(addressListName: AddressListName, addressSourceName: Option[AddressSourceName]): IO[Seq[IpEntry]] =
    client.expect[Seq[IpEntry]](Request[IO](method = Method.POST, uri = uri"/api/entries").withEntity((addressListName, addressSourceName))) // TODO

  def updateEntryEnabled(list: AddressListName, entry: IpEntry, enabled: Boolean): IO[Unit] =
    client.expect[Unit](Request[IO](method = Method.POST, uri = uri"/api/entries/enabled").withEntity((list, entry, enabled)))
}
