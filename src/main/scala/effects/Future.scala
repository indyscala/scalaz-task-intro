package effects

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

object FutureDemo extends App {
  object TheNukes {
    def launch() { println("mushroom cloud") }
  }
  import scala.concurrent.ExecutionContext.Implicits.global

  println("Apply")
  val f = Future { TheNukes.launch() }
  Await.result(f, 10.seconds)
  Await.result(f, 10.seconds)
}


