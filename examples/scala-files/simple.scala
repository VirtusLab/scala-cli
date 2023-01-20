//> using lib "com.lihaoyi::pprint:0.8.1"

object Test {
  def something[F[_]] = ()
  def msg             = "Hello"
  def main(args: Array[String]): Unit = {
    pprint.log(args)
    pprint.log(msg)
  }
}
