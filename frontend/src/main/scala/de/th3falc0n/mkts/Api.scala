package de.th3falc0n.mkts

import cats.effect.IO
import de.th3falc0n.mkts.Models.{Blocklist, IpEntry}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dom.FetchClientBuilder
import org.http4s.implicits._
import org.http4s.{Method, Request}

object Api {
  private lazy val client = FetchClientBuilder[IO].create

  def lists: IO[Seq[Blocklist]] =
    client.expect[Seq[Blocklist]](Request[IO](method = Method.GET, uri = uri"/api/lists"))

  def deleteList(list: Blocklist): IO[Unit] =
    client.expect[Unit](Request[IO](method = Method.DELETE, uri = uri"/api/lists").withEntity(list))

  def addList(list: Blocklist): IO[Unit] =
    client.expect[Unit](Request[IO](method = Method.PUT, uri = uri"/api/lists").withEntity(list))

  def entries(list: Blocklist): IO[Seq[IpEntry]] =
    client.expect[Seq[IpEntry]](Request[IO](method = Method.POST, uri = uri"/api/entries").withEntity(list))

  def updateEntryEnabled(list: Blocklist, entry: IpEntry, enabled: Boolean): IO[Unit] =
    client.expect[Unit](Request[IO](method = Method.POST, uri = uri"/api/entries/enabled").withEntity((list, entry, enabled)))
}
