package de.th3falc0n.mkts

case class Sorting[Model, T](name: String,
                             f: Model => T,
                             reverse: Boolean)
                            (implicit val ordering: Ordering[T]) {
  def sort(seq: Seq[Model]): Seq[Model] = seq.sortBy(f)(
    if (reverse) ordering.reverse
    else ordering
  )
}

object Sorting {
  def toggle[Model, T](sorting: Option[Sorting[Model, _]])
                      (name: String,
                       f: Model => T)
                      (implicit ordering: Ordering[T]): Option[Sorting[Model, T]] =
    (sorting.find(e => e.name == name).map(_.reverse) match {
      case None => Some(false)
      case Some(false) => Some(true)
      case Some(true) => None
    }).map { reverse =>
      Sorting(name, f, reverse)
    }
}
