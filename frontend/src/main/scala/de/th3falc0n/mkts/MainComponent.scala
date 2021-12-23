package de.th3falc0n.mkts

import cats.effect.IO
import de.th3falc0n.mkts.Models.{AddressList, AddressListName}
import japgolly.scalajs.react
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.ScalaComponent.BackendScope
import japgolly.scalajs.react.util.EffectCatsEffect._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html

import scala.concurrent.duration._

object MainComponent {
  case class Props()

  case class State(
                    entries: Option[Seq[AddressList]],
                    active: Option[AddressListName],
                  )

  object State {
    val empty: State = State(None, None)
  }

  class Backend($: BackendScope[Props, State]) {
    private def fetchState: IO[Unit] =
      for {
        entries <- Api.lists
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
        ^.cls := "my-4 d-flex flex-row",
        <.div(
          ^.cls := "d-flex flex-column px-2",
          <.div(^.cls := "list-group",
            <.a(^.href := "#", ^.cls := s"list-group-item list-group-item-action ${if (state.active.isEmpty) "active" else ""}",
              ^.onClick --> {
                $.modStateAsync(_.copy(active = None))
              },
              <.h5(^.cls := "mb-1", "Global Config")
            ),
            state.entries.getOrElse(Seq.empty).toVdomArray(entry =>
              <.a(^.href := "#", ^.cls := s"list-group-item list-group-item-action pe-2 ${if (state.active.contains(entry.name)) "active" else ""}",
                ^.onClick --> {
                  $.modStateAsync(_.copy(active = Some(entry.name)))
                },
                entry.name.string,
                <.button(^.cls := "btn btn-danger ms-3",
                  ^.float := "right",
                  <.i(^.cls := "bi bi-trash-fill"),
                  ^.onClick ==> { e =>
                    e.stopPropagation()
                    Api.deleteList(entry.name) >>
                      fetchState
                  }
                )
              )
            )
          ),
          {
            val inputRef = react.Ref[html.Input]
            <.div(^.cls := "d-flex flex-row mt-2",
              <.input(^.cls := "form-control flex-fill").withRef(inputRef),
              <.button(^.cls := "btn btn-primary ms-1",
                <.i(^.cls := "bi bi-plus"),
                ^.onClick --> {
                  inputRef.get.to[IO].flatMap {
                    case Some(input) =>
                      val value = input.value
                      input.value = ""
                      Api.putList(AddressList(AddressListName(value), Seq.empty, 10.minutes)) >>
                        fetchState
                  }
                }
              )
            )
          }
        ),
        <.div(
          ^.cls := "d-flex flex-column flex-fill",
          state.active.flatMap(e => state.entries.flatMap(_.find(_.name == e))) match {
            case Some(activeEntry) =>
              AddressSourceSelectorComponent.Component(AddressSourceSelectorComponent.Props(
                activeEntry,
                fetchState,
              ))

            case None =>
              GlobalConfigComponent.Component(GlobalConfigComponent.Props())
          }
        )
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
