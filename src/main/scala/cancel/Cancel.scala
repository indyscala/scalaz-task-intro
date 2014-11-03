package cancel

import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}
import scalaz.{\/-, -\/}
import scalaz.concurrent.Task


object Cancel extends App {
  object TheNukes {
    def launch() {
      println("mushroom cloud")
    }
  }

  val neverMind = new AtomicBoolean(false)
  Task {
    Thread.sleep(500)
    neverMind.set(true)
  }.runAsync {
    case -\/(t) => t.printStackTrace()
    case \/-(()) => println("Cancelled")
  }

  val countdown = new AtomicLong(10000000)
  def t: Task[Unit] = Task.delay {
    if (countdown.decrementAndGet() == 0)
      TheNukes.launch()
  }.flatMap { _ => t }

  t.runAsyncInterruptibly({
    case -\/(t) => t.printStackTrace()
    case \/-(()) => println("Completed")
  }, neverMind)

  Thread.sleep(5000L)
}
