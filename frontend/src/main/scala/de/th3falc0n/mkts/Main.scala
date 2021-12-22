package de.th3falc0n.mkts

import org.scalajs.dom

object Main {
  def main(args: Array[String]): Unit = {
    MainComponent.Component(MainComponent.Props())
      .renderIntoDOM(dom.document.getElementById("root"))
  }
}
