package scala.build.internal

// adapted from https://github.com/com-lihaoyi/Ammonite/blob/9be39debc367abad5f5541ef58f4b986b2a8d045/amm/util/src/main/scala/ammonite/util/Model.scala#L256-L266

case class ImportTree(
  prefix: Seq[String],
  mappings: Option[ImportTree.ImportMapping],
  start: Int,
  end: Int
) {
  lazy val strippedPrefix: Seq[String] =
    prefix.takeWhile(_(0) == '$').map(_.stripPrefix("$"))
}

object ImportTree {
  type ImportMapping = Seq[(String, Option[String])]
}
