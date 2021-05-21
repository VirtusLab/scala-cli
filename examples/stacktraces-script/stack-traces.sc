
def something(n: Int): String =
  if (n % 10 == 0) sys.error("nope")
  else n.toString

try {
  for (i <- 1 until 100)
    println(something(i))
} catch {
  case e: Exception =>
    throw new Exception("Caught exception during processing", e)
}
