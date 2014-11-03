package sitemon

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

object FutureDemo extends App {
  import scala.concurrent.ExecutionContext.Implicits.global
  case class Response(status: Int)
  def get(url: String) = Future {
    Thread.sleep(Random.nextInt(2000))
    Response(200)
  }

  def isDown(url: String): Future[Boolean] = get(url)
    .map { _.status != 200 } // If we got a response, was it a 200?
    .recover { case e => e.printStackTrace(); false}
  val sites = Seq("http://twitter.com", "http://github.com/")
  val panic = Future.fold(sites.map(isDown))(false)(_ || _)
  println("Panic: "+Await.result(panic, 10.seconds))
}


