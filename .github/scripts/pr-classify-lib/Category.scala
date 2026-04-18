package prclassify

/** Change categories recognized by classify-changes. The `key` is what gets written as the KEY in
  * KEY=VALUE outputs and what downstream consumers (ci.yml SHOULD_RUN expressions,
  * build-pr-classification-comment) look up.
  */
enum Category(val key: String):
  case Code         extends Category("code")
  case Docs         extends Category("docs")
  case Ci           extends Category("ci")
  case FormatConfig extends Category("format_config")
  case Benchmark    extends Category("benchmark")
  case Gifs         extends Category("gifs")
  case MillWrapper  extends Category("mill_wrapper")

object Category:

  /** Display order used in outputs and summaries. */
  val ordered: Seq[Category] = values.toIndexedSeq

  /** Classify a single changed-file path. A file can belong to zero or one category; if it belongs
    * to none it's ignored. Kept in sync with the original bash `case "$file" in ... esac` rules.
    */
  def forPath(path: String): Option[Category] = path match
    case p if p.startsWith("modules/") || p == "build.mill" || p.startsWith("project/") =>
      Some(Code)
    case p if p.startsWith("website/")       => Some(Docs)
    case p if p.startsWith(".github/")       => Some(Ci)
    case ".scalafmt.conf" | ".scalafix.conf" => Some(FormatConfig)
    case p if p.startsWith("gcbenchmark/")   => Some(Benchmark)
    case p if p.startsWith("gifs/")          => Some(Gifs)
    case "mill" | "mill.bat"                 => Some(MillWrapper)
    case _                                   => None

  def fromKey(key: String): Option[Category] = values.find(_.key == key)
