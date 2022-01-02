package de.th3falc0n.mkts

import cats.effect.IO
import de.th3falc0n.mkts.Models.{AddressList, AddressSource, AddressSourceName}
import japgolly.scalajs.react
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.ScalaComponent.BackendScope
import japgolly.scalajs.react.util.EffectCatsEffect._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html

object AddressSourceSelectorComponent {
  case class Props(addressList: AddressList,
                   onChange: IO[Unit])

  case class State(active: Option[AddressSourceName])

  object State {
    val empty: State = State(None)
  }

  class Backend($: BackendScope[Props, State]) {
    def componentDidUpdate(prevProps: Props): IO[Unit] = {
      if ($.props.unsafeRunSync().addressList.name != prevProps.addressList.name)
        $.modStateAsync(_.copy(active = None))
      else
        IO.unit
    }

    def render: VdomElement = {
      val props = $.props.unsafeRunSync()
      val state = $.state.unsafeRunSync()

      <.div(
        ^.cls := "d-flex flex-row flex-wrap h-100 gap-2",
        <.div(
          ^.cls := "d-flex flex-column overflow-auto",
          <.div(^.cls := "list-group",
            <.div(
              ^.cls := "list-group-item",
              <.h5(^.cls := "mb-1", "Sources"),
            ),
            <.a(^.key := "all", ^.href := "#",
              ^.cls := s"list-group-item list-group-item-action ${if (state.active.isEmpty) "active" else ""}",
              ^.onClick --> {
                $.modStateAsync(_.copy(active = None))
              },
              <.h6(^.cls := "mb-1", "All")
            ),
            props.addressList.sources.toVdomArray(entry =>
              <.a(^.key := s"entry-${entry.name.string}", ^.href := "#",
                ^.cls := s"list-group-item list-group-item-action pe-2 ${if (state.active.contains(entry.name)) "active" else ""}",
                ^.onClick --> {
                  $.modStateAsync(_.copy(active = Some(entry.name)))
                },
                entry.name.string,
                <.button(^.cls := "btn btn-danger ms-3",
                  ^.float := "right",
                  <.i(^.cls := "bi bi-trash-fill"),
                  ^.onClick ==> { e =>
                    e.stopPropagation()
                    Api.deleteSource(props.addressList.name, entry.name) >>
                      props.onChange
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
                  inputRef.foreach { input =>
                    val value = input.value
                    input.value = ""
                    Api.putSource(props.addressList.name, AddressSource(AddressSourceName(value))) >>
                      props.onChange
                  }
                }
              )
            )
          }
        ),
        <.div(
          ^.cls := "d-flex flex-column flex-fill h-100",
          IpListComponent.Component(IpListComponent.Props(
            props.addressList,
            state.active.flatMap(e => props.addressList.sources.find(_.name == e))
          ))
        )
      )
    }
  }

  val Component =
    ScalaComponent.builder[Props]
      .initialState(State.empty)
      .backend(new Backend(_))
      .render(_.backend.render)
      .componentDidUpdate(e => e.backend.componentDidUpdate(e.prevProps))
      .build
}
