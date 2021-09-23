package scala.build.options

import java.util.Locale

sealed abstract class Platform(val repr: String) extends Product with Serializable

object Platform {
  case object JVM    extends Platform("JVM")
  case object JS     extends Platform("JS")
  case object Native extends Platform("Native")

  def normalize(p: String): String =
    p.toLowerCase(Locale.ROOT) match {
      case "scala.js" | "scala-js" | "scalajs" | "js" => "js"
      case "scala-native" | "scalanative" | "native"  => "native"
      case "jvm"                                      => "jvm"
      case _                                          => p
    }
  def parse(p: String): Option[Platform] =
    p match {
      case "jvm"    => Some(JVM)
      case "js"     => Some(JS)
      case "native" => Some(Native)
      case _        => None
    }

  private def parseSpec0(
    l: List[String],
    acc: Set[Platform]
  ): Option[Set[Platform]] =
    l match {
      case Nil      => None
      case p :: Nil => parse(p).map(p0 => acc + p0)
      case p :: "|" :: tail =>
        parse(p) match {
          case Some(p0) => parseSpec0(tail, acc + p0)
          case None     => None
        }
      case _ => None
    }

  def parseSpec(input: Seq[String]): Option[Set[Platform]] =
    parseSpec0(input.toList, Set.empty)

}
