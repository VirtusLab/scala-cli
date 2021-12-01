package scala.build.options

import scala.util.Try

object ParseJavaVersion {
  def parse(input: String): Option[Int] = for {
    firstMatch         <- """.*version .(1[.])?(\d+).*""".r.findFirstMatchIn(input)
    versionNumberGroup <- Option(firstMatch.group(2))
    versionInt         <- Try(versionNumberGroup.toInt).toOption
  } yield versionInt
}
