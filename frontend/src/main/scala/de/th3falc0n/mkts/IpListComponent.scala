package de.th3falc0n.mkts

import cats.effect.IO
import cats.syntax.option._
import de.th3falc0n.mkts.Models.{AddressList, AddressSource, IPListEntry}
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.ScalaComponent.BackendScope
import japgolly.scalajs.react.internal.CoreGeneral.ReactEventFromInput
import japgolly.scalajs.react.util.EffectCatsEffect._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._

import scala.concurrent.duration._
import scala.util.chaining._

object IpListComponent {
  case class Props(
                    selectedAddressList: AddressList,
                    selectedAddressSource: Option[AddressSource],
                  )

  case class State(
                    entries: Option[Seq[IPListEntry]],
                    sorting: Option[Sorting[IPListEntry, _]],
                    filter: String,
                    selected: Option[IPListEntry],
                  ) {
    def toggleSorting[T](name: String, f: IPListEntry => T)(implicit ordering: Ordering[T]): State =
      copy(sorting = Sorting.toggle(sorting)(name, f)).tap(_.entriesSorted)

    lazy val entriesSorted: Seq[IPListEntry] = {
      val filterLowerCase = filter.toLowerCase
      entries.getOrElse(Seq.empty)
        .filter(_.toString.contains(filterLowerCase))
        .pipe(e => sorting.fold(e)(_.sort(e)))
    }
  }

  object State {
    val empty: State = State(None, None, "", None)
  }

  class Backend($: BackendScope[Props, State]) {
    private def fetchState: IO[Unit] =
      for {
        props <- $.props.to[IO]
        entries <- Api.entries(props.selectedAddressList.name, props.selectedAddressSource.map(_.name))
        _ <- $.modStateAsync(_.copy(entries = Some(entries)).tap(_.entriesSorted))
      } yield ()

    def componentDidMount: IO[Unit] = {
      lazy val tick: IO[Unit] =
        fetchState >>
          tick.delayBy(8.seconds)

      tick
    }

    def componentDidUpdate(prevProps: Props): IO[Unit] = {
      if ($.props.unsafeRunSync() != prevProps)
        fetchState
      else
        IO.unit
    }

    def render: VdomElement = {
      val state = $.state.unsafeRunSync()

      <.div(
        ^.cls := "d-flex flex-column flex-fill overflow-auto",
        {
          case class Col[T](name: String,
                            f: IPListEntry => T,
                            cell: IPListEntry => VdomElement,
                            label: String = null,
                            shrink: Boolean = false)
                           (implicit ordering: Ordering[T]) {
            def header: VdomElement =
              <.th(
                ^.key := name,
                ^.scope := "col",
                ^.cls := "sortable-table-header",
                ^.width :=? Option.when(shrink)("0"),
                ^.onClick --> $.modStateAsync(_.toggleSorting(name, f)),
                <.div(
                  ^.cls := "d-flex flex-row pe-3",
                  <.div(
                    ^.whiteSpace := "nowrap",
                    Option(label).getOrElse[String](name)
                  ),
                  <.div(
                    ^.position := "relative",
                    state.sorting.filter(_.name == name).map(sorting =>
                      <.i(^.cls := s"bi ${if (sorting.reverse) "bi-caret-up-fill" else "bi-caret-down-fill"} ms-1", ^.position := "absolute")
                    )
                  )
                )
              )
          }

          val columns = Seq[Col[_]](
            Col(s"IP (${state.entriesSorted.size})", _.ip.host,
              e => <.th(^.key := "ip", ^.scope := "row", e.ip.toString),
              label = s"IP (${state.entriesSorted.size})"
            ),
            Col("# of Hosts", _.ip.numberOfHosts,
              e => <.td(^.key := "number", e.ip.numberOfHosts)
            ),
            Col("Comment", _.comment,
              e => <.td(^.key := "comment", e.comment)
            )
          )

          <.table(^.cls := "table table-sm table-striped table-hover",
            <.thead(
              <.tr(columns.toVdomArray(_.header))
            ),
            <.tbody(
              <.tr(
                <.td(
                  ^.colSpan := columns.size,
                  <.input(
                    ^.id := "search",
                    ^.cls := "align-self-center form-control",
                    ^.tpe := "text",
                    ^.placeholder := "Search IP...",
                    ^.onChange ==> { e: ReactEventFromInput =>
                      val value = e.target.value
                      $.modState(_.copy(filter = value))
                    }
                  )
                )
              ),
              state.entriesSorted.toVdomArray { entry =>
                val active = state.selected.contains(entry)
                <.tr(
                  ^.key := entry.toString,
                  ^.cls := s"selectable-table-row ${if (active) "active" else ""}",
                  ^.onClick -->? Option.when(!active) {
                    $.modStateAsync(_.copy(selected = entry.some))
                  },
                  columns.toVdomArray(_.cell(entry))
                )
              }
            )
          )
        }
      )
    }
  }

  val Component =
    ScalaComponent.builder[Props]
      .initialState(State.empty)
      .backend(new Backend(_))
      .render(_.backend.render)
      .componentDidMount(_.backend.componentDidMount)
      .componentDidUpdate(e => e.backend.componentDidUpdate(e.prevProps))
      .build
}
