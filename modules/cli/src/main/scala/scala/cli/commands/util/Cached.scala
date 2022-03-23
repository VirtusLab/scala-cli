package scala.cli.commands.util

object cached {
  private val cache = collection.concurrent.TrieMap[Any, Any]()

  def apply[V](from: Any)(create: => V): V = cache.getOrElseUpdate(from, create).asInstanceOf[V]
}
