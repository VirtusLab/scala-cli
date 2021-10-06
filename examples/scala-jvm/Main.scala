// File was generated from based on docs/cookbooks/scala-jvm.md, do not edit manually!

import java.nio.file.Files
import java.nio.file.Paths

object Main extends App {
  val dest = Files.createTempDirectory("scala-cli-demo").resolve("hello.txt")
  val filePath = Files.writeString(dest, "Hello from ScalaCli")
  val fileContent: String = Files.readString(filePath)
  println(fileContent)
}