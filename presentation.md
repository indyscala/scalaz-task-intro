# scalaz.concurrent.Task

## Why?  Why not?

### IndyScala, November 3, 2014

---

# Probably 1 of 3?  Yeah, probably.

1. **scalaz.concurrent.Task** (November, 2014)
2. scalaz-stream (???)
3. http4s (???)

---

# Ye Olden Tymes 

*Way back in Scala 2.9.2*

* java.util.concurrent.Future ☠☠☠
    * com.google.common.util.concurrent.ListenableFuture
* scala.actors.Future
* akka.dispatch.Future
* com.twitter.util.Future
* scalaz.concurrent.Promise

---

# That begat [sff4s](https://github.com/eed3si9n/sff4s)

    !scala
    val factory = sff4s.impl.ActorsFuture
    val f = factory future {
       Thread.sleep(1000)
       1
    }
    val g = f map { _ + 1 }
    g(2000) 

... which was nice for unopinionated frameworks, but never really caught on.

---

# Enter SIP-14

* [Futures and Promises](http://docs.scala-lang.org/sips/completed/futures-promises.html)
* Introduced in Scala 2.10
* Backported to Scala 2.9.3
* You know it as `scala.concurrent.Future`
    * You know, the thing `?` returns from an actor.

---

# Standardization???

* java.util.concurrent.Future ☠☠☠
    * com.google.common.util.concurrent.ListenableFuture
    * java.util.concurrent.CompletableFuture
* <strike>scala.actors.Future</strike>
* <strike>scalaz.concurrent.Promise</strike>
* <strike>akka.dispatch.Future</strike>
* com.twitter.util.Future
* *scala.concurrent.Future*
* <strike>scalaz.concurrent.Promise</strike>
* *scalaz.concurrent.Task*

---

# ![This is why we can't have nice things](images/nice-things.jpeg)

---

# Wrong presentation

* Tomorrow night is why we can't have nice things.  
* For concurrent programming in Scala, we *do* have two rather nice things.

---

# A review of scala.concurrent.Future

    !scala
    def isOk(url: String): Future[Boolean] = get(url)
      .map { _.status == 200 } // If we got a response, was it a 200?
      .recover { case e => e.printStackTrace(); false}
    val sites = Seq("http://twitter.com", "http://github.com/")
    val panic = Future.fold(sites.map(isOk))(false) {
      (acc, isSiteOk) => acc || !isSiteOk
    }
    println("Panic: "+Await.result(panic, 10.seconds))

* A Future is an asynchronous computation.
* A Future tries (that is, `scala.util.Try`s) to complete that computation.
* Yup.  Future is a monad.

---

# Enter scalaz.concurrent.Future

* Like Scala Future, a Scalaz Future is an asynchronous computation.
* Yup.  This Future is a monad, too.
* But there's no error handling in this monad.

---

# Enter scalaz.concurrent.<strike>Future</strike>Task

    !scala
    def isDown(url: String): Future[Boolean] = get(url)
      .map { _.status != 200 } // If we got a response, was it a 200?
      .recover { case e => e.printStackTrace(); false}
    val sites = Seq("http://twitter.com", "http://github.com/")
    val panic = Future.fold(sites.map(isDown))(false)(_ || _)
    println("Panic: "+Await.result(panic, 10.seconds))

* A `Task[A]` is a thin wrapper around `Future[Throwable \/ A]`. 
    * `scalaz.\/` is commonly pronounced as *either*.
        * Because *either* it worked or it didn't.
        * Yup. `\/` is a monad.  (scala.Either, incidentally, is not.)
    * Yup.  `Task` is a monad.

---

# So the difference is just in method names?

* This is just more of that NIH Syndrome that Scala is famous for, isn't it?

![No.](images/no.jpg)

---

# Future.apply immediately submits to executor

    !scala
    Future { TheNukes.launch() }

![Mushroom cloud](images/shroom.jpg)

<font size="-3">By Federal Government of the United States [Public domain], via Wikimedia Commons</font>

---

# Tasks don't run ...

    !scala
    Task { TheNukes.launch() }

![Bucolic scene](images/bucolic.jpg)

<font size="-3">Alison Rawson [CC-BY-SA-2.0](http://creativecommons.org/licenses/by-sa/2.0), via Wikimedia Commons</font>

---

# ... until you ask them to.

    !scala
    val task = Task { TheNukes.launch() }
    headForShelter()
    task.run

![Mushroom cloud](images/shroom.jpg)

---

# A Future's side effects run once.

    !scala
    val f = Future { TheNukes.launch() }
    Await.result(f, 1.second)
    Await.result(f, 1.second)

![Mushroom cloud](images/shroom.jpg)

---

# A Task's side effects run each time.

    !scala
    val t = Task { TheNukes.launch() }
    t.run
    t.run

![Mushroom cloud](images/shroom.jpg) 
![Mushroom cloud](images/shroom.jpg) 

---

# I like how Future does it.

Fine.

    !scala
    val t = Task.unsafeStart { TheNukes.launch() }
    t.run
    t.run

![Mushroom cloud](images/shroom.jpg) 

---

# Site monitor reprise

    !scala
    val f = Future.fold(sites.map(isDown))(false)(_ || _)
    val t = Task.reduceUnordered[Boolean, Boolean](sites.map(isDown))

* `f` will always contain the same value when completed.
* `t` will rerun the monitoring and produce a current value on each run.

---

# Future methods that submit to executor:

* `andThen`
* `collect`
* `filter`
* `flatMap`
* `foreach`
* `onFailure`
* `onSuccess`
* `recover`
* `transform`
* `withFilter`

---

# Task methods that submit to executor:

[This slide intentionally left blank]

---

# Execution model

* Future makes you opt out of thread hopping by explicitly setting an
  executor that stays on the same thread.
    * It's easy to saturate your thread pool. 
    * It's also easy to waste time submitting trivial work to it.
    * For optimal performance, swap custom execution contexts into the hotspots.

* Task makes you opt into thread hopping.
    * It's easy to accidentally only use one core in your complicated
    * But it'll work really efficiently on that one core.
    * For optimal performance, salt to taste with Task.apply and Task.fork

* Bottom line: both ultimately offer the same control, promote a different
  kind of naive mistake.

---

# Hey, what about that Twitter thing?

* Twitter's Future is cancellable.
* Scala's Future is not cancellable.
* Scalaz's Task is cancellable.

---

# Tasks are cancellable

    !scala
    val neverMind = new AtomicBoolean(false)
    Task {
      Thread.sleep(500)
      neverMind.set(true)
    }.runAsync {
      case -\/(t) => t.printStackTrace()
      case \/-(()) => println("Cancelled")
    }
    val t = Task.delay {
      Thread.sleep(1000)
      TheNukes.launch()
    }
    t.runAsyncInterruptibly({
      case -\/(t) => t.printStackTrace()
      case \/-(()) => println("Completed")
    }, neverMind)

![bucolic cow](images/bucolic.jpg) 
  
---

# ... but not to the point of nuclear safety

    !scala
    val neverMind = new AtomicBoolean(false)
    Task {
      Thread.sleep(500)
      neverMind.set(true)
    }.runAsync {
      case -\/(t) => t.printStackTrace()
      case \/-(()) => println("Cancelled")
    }
  
    val t = Task.delay {
      Thread.sleep(1000)
      TheNukes.launch()
    }
    t.runAsyncInterruptibly({
      case -\/(t) => t.printStackTrace()
      case \/-(()) => println("Completed")
    }, neverMind)
  
![Mushroom cloud](images/shroom.jpg) 

---

# Futures are for Typesafe people and Tasks are for Typelevel people?

---

# Usually, but it doesn't have to be that way.

![handshake](images/handshake.jpg) 

---

# Convert a Task to a Future

    !scala
    val p = Promise[A]()
    task.runAsync {
      case \/-(a) => p.success(a)
      case -\/(t) => p.failure(t)
    }
    p.future

---

# Convert a Future to a Task

    !scala
    Task.async { f =>
      future.onComplete {
        case Success(a) => f(right(a))
        case Failure(t) => f(left(t))
      }
    }

---

# Futher reading

* [SIP-14](http://docs.scala-lang.org/sips/completed/futures-promises.html)
* [Task, the missing documentation](http://timperrett.com/2014/07/20/scalaz-task-the-missing-documentation/)

---

# Ross A. Baker

- [@rossabaker](http://twitter.com/rossabaker)
- Co-organizer, [IndyScala](http://github.com/indyscala/)
- Principal Cloud Engineer, [CrowdStrike](http://www.crowdstrike.com/)
    - We're [hiring](http://www.crowdstrike.com/about-us/careers/)

