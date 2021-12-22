package de.th3falc0n.mkts

import cats.effect.IO
import de.th3falc0n.mkts.Models.IpEntry
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dom.FetchClientBuilder
import org.http4s.implicits._
import org.http4s.{Method, Request}

object Backend {
  private lazy val client = FetchClientBuilder[IO].create

  def entries: IO[Seq[IpEntry]] =
    client.expect[Seq[IpEntry]](Request[IO](method = Method.GET, uri = uri"/api/entries"))

  def deleteEntry(entry: IpEntry): IO[Unit] = IO.unit
}
