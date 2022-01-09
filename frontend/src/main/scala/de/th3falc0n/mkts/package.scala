package de.th3falc0n

import cats.effect.IO
import de.lolhens.remoteio.Rest.RestClientImpl
import org.http4s.dom.FetchClientBuilder
import org.http4s.implicits._

package object mkts {
  implicit val restClient: RestClientImpl[IO] = RestClientImpl[IO](
    FetchClientBuilder[IO].create,
    uri"/api"
  )
}
