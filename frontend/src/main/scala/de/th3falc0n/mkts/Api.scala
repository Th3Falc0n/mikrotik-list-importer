package de.th3falc0n.mkts

import cats.effect.IO
import de.th3falc0n.mkts.Models._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dom.FetchClientBuilder
import org.http4s.implicits._
import org.http4s.{Method, Request}

object Api {
  private lazy val client = FetchClientBuilder[IO].create

  def lists: IO[Seq[AddressList]] =
    client.expect[Seq[AddressList]](Request[IO](method = Method.GET, uri = uri"/api/lists"))

  def deleteList(addressListName: AddressListName): IO[Unit] =
    client.expect[Unit](Request[IO](method = Method.DELETE, uri = uri"/api/lists").withEntity(addressListName))

  def putList(addressList: AddressList): IO[Unit] =
    client.expect[Unit](Request[IO](method = Method.PUT, uri = uri"/api/lists").withEntity(addressList))

  def deleteSource(addressListName: AddressListName, addressSourceName: AddressSourceName): IO[Unit] =
    for {
      addressListOption <- lists.map(_.find(_.name == addressListName))
      _ <- addressListOption match {
        case Some(addressList) =>
          putList(addressList.copy(sources = addressList.sources.filterNot(_.name == addressSourceName)))

        case None => IO.unit
      }
    } yield ()

  def putSource(addressListName: AddressListName, addressSource: AddressSource): IO[Unit] =
    for {
      addressListOption <- lists.map(_.find(_.name == addressListName))
      _ <- addressListOption match {
        case Some(addressList) =>
          putList(addressList.copy(sources = addressList.sources :+ addressSource))

        case None => IO.unit
      }
    } yield ()

  def entries(addressListName: AddressListName, addressSourceName: Option[AddressSourceName]): IO[Seq[IP]] =
    client.expect[Seq[IP]](Request[IO](method = Method.POST, uri = uri"/api/entries").withEntity((addressListName, addressSourceName))) // TODO

  def updateEntryEnabled(list: AddressListName, entry: IP, enabled: Boolean): IO[Unit] =
    client.expect[Unit](Request[IO](method = Method.POST, uri = uri"/api/entries/enabled").withEntity((list, entry, enabled)))
}
