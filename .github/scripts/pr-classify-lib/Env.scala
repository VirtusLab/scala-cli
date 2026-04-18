package prclassify

import java.nio.file.Paths

/** Small helpers for reading environment variables with consistent behavior:
  *   - empty strings are treated as unset,
  *   - missing required variables print a GitHub Actions `::error::` line and exit with code 1.
  */
object Env:

  def opt(name: String): Option[String] =
    sys.env.get(name).filter(_.nonEmpty)

  def required(name: String): String =
    opt(name).getOrElse:
      System.err.println(s"::error::$name is required")
      sys.exit(1)

  def requiredFile(name: String): os.Path =
    val raw  = required(name)
    val path = toAbsolutePath(raw)
    if !os.exists(path) then
      System.err.println(s"::error::$name points to non-existent file: $path")
      sys.exit(1)
    path

  def withDefault(name: String, default: String): String =
    opt(name).getOrElse(default)

  def toAbsolutePath(s: String): os.Path =
    if Paths.get(s).isAbsolute then os.Path(s) else os.Path(s, os.pwd)
