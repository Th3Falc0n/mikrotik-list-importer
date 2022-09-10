package de.th3falc0n.mkts
package repo

import Models.AddressList

import cats.effect.{ IO, Ref }

trait AddressListRepo[F[_]] {
  def list: F[Seq[AddressList]]

  def put(addressList: AddressList): F[Unit]

  def delete(addressListName: String): F[Unit]
}

object AddressListRepo {
  def inMemImpl: AddressListRepo[IO] = new AddressListRepo[IO] {
    private val addressLists = Ref.unsafe[IO, Seq[AddressList]](Seq.empty[AddressList])

    override def list: IO[Seq[AddressList]] =
      addressLists.get

    override def put(addressList: AddressList): IO[Unit] =
      addressLists.update(_.filterNot(_.name == addressList.name) :+ addressList)

    override def delete(addressListName: String): IO[Unit] =
      addressLists.update(_.filterNot(_.name == addressListName))
  }
}
