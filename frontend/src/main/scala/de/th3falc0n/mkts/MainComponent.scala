package de.th3falc0n.mkts

import cats.effect.IO
import de.th3falc0n.mkts.Models.IpEntry
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.ScalaComponent.BackendScope
import japgolly.scalajs.react.internal.CoreGeneral.ReactEventFromInput
import japgolly.scalajs.react.util.EffectCatsEffect._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html.TableCell

import scala.concurrent.duration._

object MainComponent {
  case class Props()

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
        entries <- Backend.entries
        _ <- $.modStateAsync(_.copy(entries = Some(entries)))
      } yield ()

    def componentDidMount: IO[Unit] = {
      lazy val tick: IO[Unit] =
        fetchState >>
          tick.delayBy(8.seconds)

      tick
    }

    def render: VdomElement = {
      val state = $.state.unsafeRunSync()

      <.div(
        ^.cls := "container my-4 d-flex flex-column",
        {
          val headers = Seq[VdomTagOf[TableCell]](
            <.th(^.scope := "col", "IP"),
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
                  <.td(
                    <.button(^.cls := "btn btn-danger",
                      <.i(^.cls := "bi bi-trash-fill"),
                      ^.onClick --> {
                        Backend.deleteEntry(entry) >>
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
      .build
}
