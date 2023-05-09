package scala.build
import scala.collection.mutable
object CollectionOps {
  extension [T](items: Seq[T]) {

    /** Works the same standard lib's `distinct`, but only differentiates based on the key extracted
      * by the passed function. If more than one value exists for the same key, only the first one
      * is kept, the rest is filtered out.
      *
      * @param f
      *   function to extract the key used for distinction
      * @tparam K
      *   type of the key used for distinction
      * @return
      *   the sequence of items with distinct [[items]].map(f)
      */
    def distinctBy[K](f: T => K): Seq[T] =
      if items.length == 1 then items
      else
        val seen = mutable.HashSet.empty[K]
        items.filter { item =>
          val key = f(item)
          if seen(key) then false
          else
            seen += key
            true
        }
  }
}
