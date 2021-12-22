package de.th3falc0n.mkts

import cats.effect.IO
import de.th3falc0n.mkts.Models.{Blocklist, IpEntry}
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.ScalaComponent.BackendScope
import japgolly.scalajs.react.internal.CoreGeneral.ReactEventFromInput
import japgolly.scalajs.react.util.EffectCatsEffect._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html.TableCell

import scala.concurrent.duration._

object IpListComponent {
  case class Props(list: Blocklist)

  case class State(
                    entries: Option[Seq[IpEntry]],
                    filter: String
                  )

  object State {
    val empty: State = State(None, "")
  }

  class Backend($: BackendScope[Props, State]) {
    private def fetchState: IO[Unit] =
      for {
        props <- $.props.to[IO]
        entries <- Api.entries(props.list)
        _ <- $.modStateAsync(_.copy(entries = Some(entries)))
      } yield ()

    def componentDidMount: IO[Unit] = {
      lazy val tick: IO[Unit] =
        fetchState >>
          tick.delayBy(8.seconds)

      tick
    }

    def componentDidUpdate: IO[Unit] = {
      fetchState
    }

    def render: VdomElement = {
      val props = $.props.unsafeRunSync()
      val state = $.state.unsafeRunSync()

      <.div(
        ^.cls := "d-flex flex-column flex-fill",
        {
          val headers = Seq[VdomTagOf[TableCell]](
            <.th(^.scope := "col", "IP"),
            <.th(^.scope := "col", "Enabled"),
            <.th(^.scope := "col", ^.width := "0"),
          )

          <.table(^.cls := "table",
            <.thead(
              <.tr(headers: _*)
            ),
            <.tbody(
              <.tr(
                <.td(
                  ^.colSpan := headers.size,
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
              ), {
                val filterLowerCase = state.filter.toLowerCase
                state.entries.getOrElse(Seq.empty).filter(_.string.contains(filterLowerCase))
              }.toVdomArray { entry =>
                <.tr(
                  ^.key := entry.string,
                  <.th(^.scope := "row", entry.string),
                  <.th(entry.enabled.toString),
                  <.td(
                    <.button(^.cls := "btn btn-danger",
                      <.i(^.cls := "bi bi-trash-fill"),
                      ^.onClick --> {
                        Api.updateEntryEnabled(props.list, entry, entry.enabled) >>
                          fetchState
                      }
                    )
                  ),
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
      .componentDidUpdate(_.backend.componentDidUpdate)
      .build
}
