package scala.build.options.collections

/** Contains various prefixes for different cli options. Only used in case of format:
  * `--option<value>`, without any separating ':' or white characters. If not added, those options
  * will simply not be shadowed by the cli options (resulting in errors).
  */
object PrefixedOptions {
  val javaPrefixes: Seq[String] = Seq("-Xmn", "-Xms", "-Xmx", "-Xss")
  val scalacPrefixes: Seq[String] = Seq.empty
}
