package effects

import scalaz.concurrent.Task

object TaskDemo extends App {
  object TheNukes {
    def launch() { println("mushroom cloud") }
  }

  println("Apply")
  val t = Task { TheNukes.launch() }
  t.run
  t.run
}
