package sitemon

import scala.util.Random
import scalaz.concurrent.Task

object TaskDemo extends App {
  case class Response(status: Int)
  def get(url: String) = Task {
    Thread.sleep(Random.nextInt(2000))
    Response(200)
  }

  def isDown(url: String): Task[Boolean] = get(url)
    .map { _.status != 200 } // If we got a response, was it a 200?
    .handle { case e => e.printStackTrace(); true }
  val sites = Seq("http://twitter.com", "http://github.com/")
  val panic =
    Task.reduceUnordered[Boolean, Boolean](sites.map(isDown))
  println("Panic?: "+panic.run)
}
