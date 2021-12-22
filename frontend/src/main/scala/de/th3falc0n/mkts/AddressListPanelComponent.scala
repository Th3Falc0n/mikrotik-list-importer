package de.th3falc0n.mkts

import cats.effect.IO
import de.th3falc0n.mkts.Models.{AddressList, AddressSourceName}
import japgolly.scalajs.react
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.ScalaComponent.BackendScope
import japgolly.scalajs.react.util.EffectCatsEffect._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html

object AddressListPanelComponent {
  case class Props(addressList: AddressList,
                   onChange: IO[Unit])

  case class State(active: Option[AddressSourceName])

  object State {
    val empty: State = State(None)
  }

  class Backend($: BackendScope[Props, State]) {
    def render: VdomElement = {
      val props = $.props.unsafeRunSync()
      val state = $.state.unsafeRunSync()

      <.div(
        ^.cls := "d-flex flex-row",
        <.div(
          ^.cls := "d-flex flex-column px-2",
          <.div(^.cls := "list-group",
            <.a(^.href := "#", ^.cls := s"list-group-item list-group-item-action ${if (state.active.isEmpty) "active" else ""}",
              ^.onClick --> {
                $.modStateAsync(_.copy(active = None))
              },
              <.h5(^.cls := "mb-1", "All")
            ),
            props.addressList.sources.toVdomArray(entry =>
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
                    //Api.deleteList(entry.name) >>
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
                  inputRef.get.to[IO].flatMap {
                    case Some(input) =>
                      val value = input.value
                      input.value = ""
                      //Api.putList(AddressList(AddressListName(value), Seq.empty, 10.minutes)) >>
                      props.onChange
                  }
                }
              )
            )
          }
        ),
        <.div(
          ^.cls := "d-flex flex-column flex-fill",
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
      .build
}
