package scala.cli

import java.util.concurrent.CompletableFuture

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

package object integration {

  implicit class CompletableFutureToScala[T](private val cf: CompletableFuture[T]) extends AnyVal {
    def asScala: Future[T] = {
      val p = Promise[T]()
      cf.handle { (t, ex) =>
        val res =
          if (ex == null) Success(t)
          else Failure(ex)
        p.complete(res)
      }
      p.future
    }
  }

}
