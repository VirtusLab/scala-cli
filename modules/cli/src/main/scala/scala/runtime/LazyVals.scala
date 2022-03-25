package scala.runtime

// THIS Class is copied from Scala stdlib and replace original one

/**
 * Helper methods used in thread-safe lazy vals.
 */
object LazyVals {
  val store = scala.collection.concurrent.TrieMap[(Any, Long), Long]()

  private[this] val base: Int = {
    val processors = java.lang.Runtime.getRuntime.nn.availableProcessors()
    8 * processors * processors
  }
  private[this] val monitors: Array[Object] =
    Array.tabulate(base)(_ => new Object)

  private def getMonitor(obj: Object, fieldId: Int = 0) = {
    var id = (java.lang.System.identityHashCode(obj) + fieldId) % base

    if (id < 0) id += base
    monitors(id)
  }

  private final val LAZY_VAL_MASK = 3L
  private final val debug = false

  /* ------------- Start of public API ------------- */

  final val BITS_PER_LAZY_VAL = 2L

  def STATE(cur: Long, ord: Int): Long = {
    val r = (cur >> (ord * BITS_PER_LAZY_VAL)) & LAZY_VAL_MASK
    if (debug)
      println(s"STATE($cur, $ord) = $r")
    r
  }

  def CAS(t: Object, offset: Long, e: Long, v: Int, ord: Int): Boolean = {
    if (debug)
      println(s"CAS($t, $offset, $e, $v, $ord)")
    val mask = ~(LAZY_VAL_MASK << ord * BITS_PER_LAZY_VAL)
    val n = (e & mask) | (v.toLong << (ord * BITS_PER_LAZY_VAL))
    if (store.get((t, offset)).getOrElse(0) == n) {
      store.put((t, offset), n)
      false
    } else true
  }

  def setFlag(t: Object, offset: Long, v: Int, ord: Int): Unit = {
    if (debug)
      println(s"setFlag($t, $offset, $v, $ord)")
    var retry = true
    while (retry) {
      val cur = get(t, offset)
      if (STATE(cur, ord) == 1) retry = !CAS(t, offset, cur, v, ord)
      else {
        // cur == 2, somebody is waiting on monitor
        if (CAS(t, offset, cur, v, ord)) {
          val monitor = getMonitor(t, ord)
          monitor.synchronized {
            monitor.notifyAll()
          }
          retry = false
        }
      }
    }
  }

  def wait4Notification(t: Object, offset: Long, cur: Long, ord: Int): Unit = {
    if (debug)
      println(s"wait4Notification($t, $offset, $cur, $ord)")
    var retry = true
    while (retry) {
      val cur = get(t, offset)
      val state = STATE(cur, ord)
      if (state == 1) CAS(t, offset, cur, 2, ord)
      else if (state == 2) {
        val monitor = getMonitor(t, ord)
        monitor.synchronized {
          if (STATE(get(t, offset), ord) == 2) // make sure notification did not happen yet.
            monitor.wait()
        }
      }
      else retry = false
    }
  }

  def get(t: Object, off: Long): Long = {
    if (debug)
      println(s"get($t, $off)")
    store.get((t, off)).getOrElse(0)
    //getFromHandle(handles(off.toInt), t).asInstanceOf[Long]
    //unsafe.getLongVolatile(t, off)
  }

  def getOffset(clz: Class[_], name: String): Long = {
    val index = scala.util.Random.nextLong()
    if (debug)
      println(s"getOffset($clz, $name) = $index")
    index
  }

  object Names {
    final val state = "STATE"
    final val cas = "CAS"
    final val setFlag = "setFlag"
    final val wait4Notification = "wait4Notification"
    final val get = "get"
    final val getOffset = "getOffset"
  }
}