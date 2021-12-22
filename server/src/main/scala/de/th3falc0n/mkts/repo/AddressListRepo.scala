package de.th3falc0n.mkts.repo

import cats.effect.{IO, Ref}
import de.th3falc0n.mkts.Models.{AddressList, AddressListName}
import io.circe.syntax._
import org.slf4j.LoggerFactory

import java.nio.file.{Files, Path}

trait AddressListRepo[F[_]] {
  def list: F[Seq[AddressList]]

  def put(addressList: AddressList): F[Unit]

  def delete(addressListName: AddressListName): F[Unit]
}

object AddressListRepo {
  def inMemImpl: AddressListRepo[IO] = new AddressListRepo[IO] {
    private val addressLists = Ref.unsafe[IO, Seq[AddressList]](Seq.empty[AddressList])

    override def list: IO[Seq[AddressList]] =
      addressLists.get

    override def put(addressList: AddressList): IO[Unit] =
      addressLists.update(_.filterNot(_.name == addressList.name) :+ addressList)

    override def delete(addressListName: AddressListName): IO[Unit] =
      addressLists.update(_.filterNot(_.name == addressListName))
  }

  def jsonFileImpl(file: Path): AddressListRepo[IO] = new AddressListRepo[IO] {
    private val logger = LoggerFactory.getLogger(getClass)
    private val addressLists = Ref.unsafe[IO, Option[Seq[AddressList]]](None)

    def write(addressLists: Seq[AddressList]): IO[Unit] =
      IO(Files.writeString(file, addressLists.asJson.spaces2))

    override def list: IO[Seq[AddressList]] =
      addressLists.get.flatMap {
        case Some(lists) => IO.pure(lists)
        case None =>
          IO {
            logger.info(s"Loading AddressList file: $file")
            val string = Files.readString(file)
            io.circe.parser.decode[Seq[AddressList]](string).toTry.get
          }.flatTap(e => addressLists.set(Some(e)))
      }

    override def put(addressList: AddressList): IO[Unit] =
      list >>
        addressLists.updateAndGet(e => Some(e.get.filterNot(_.name == addressList.name) :+ addressList))
          .flatTap(e => write(e.get))
          .void

    override def delete(addressListName: AddressListName): IO[Unit] =
      list >>
        addressLists.updateAndGet(e => Some(e.get.filterNot(_.name == addressListName)))
          .flatTap(e => write(e.get))
          .void
  }
}
