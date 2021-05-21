val sv = scala.util.Properties.versionNumberString

def printMessage(): Unit =
  val message = s"Hello from Scala ${sv}, Java ${System.getProperty("java.version")}"
  println(message)

printMessage()
