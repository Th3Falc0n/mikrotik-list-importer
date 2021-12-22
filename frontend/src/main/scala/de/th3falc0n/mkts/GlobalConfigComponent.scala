package de.th3falc0n.mkts

import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.ScalaComponent.BackendScope
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._

object GlobalConfigComponent {
  case class Props()

  case class State()

  object State {
    val empty: State = State()
  }

  class Backend($: BackendScope[Props, State]) {
    def render: VdomElement = {
      <.div(
        "Global Config"
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
