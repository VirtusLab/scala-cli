import ai.kien.python._

object Test {
  def main(args: Array[String]): Unit = {
    val javaVer  = sys.props("java.version")
    val scalaVer = scala.util.Properties.versionNumberString
    println(s"Hello from Java $javaVer, Scala standard library $scalaVer")
    val jnaLibPath = Python()
      .scalaPyProperties
      .getOrElse(Map.empty[String, String])
      .getOrElse("jna.library.path", "")
    println(s"You should set jna.library.path to $jnaLibPath")
  }
}
