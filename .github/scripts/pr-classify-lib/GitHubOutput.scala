package prclassify

import java.util.UUID

/** Helpers for writing to `$GITHUB_OUTPUT` and `$GITHUB_STEP_SUMMARY`. All methods are no-ops when
  * the corresponding env var is unset, which keeps the scripts usable outside GitHub Actions (e.g.
  * local smoke-testing).
  */
object GitHubOutput:

  def writeScalar(key: String, value: String): Unit =
    Env.opt(EnvNames.GitHubOutput).foreach: path =>
      os.write.append(Env.toAbsolutePath(path), s"$key=$value\n")

  /** Writes a GitHub Actions heredoc-style multi-line output. Uses a random delimiter so values
    * never accidentally collide with the closing marker.
    */
  def writeMultiline(key: String, values: Iterable[String]): Unit =
    Env.opt(EnvNames.GitHubOutput).foreach: path =>
      val delimiter = s"EOF_${UUID.randomUUID().toString.replace("-", "")}"
      val payload   =
        (Iterator.single(s"$key<<$delimiter") ++ values.iterator ++ Iterator.single(delimiter))
          .mkString("", "\n", "\n")
      os.write.append(Env.toAbsolutePath(path), payload)

  /** Appends `text` to `$GITHUB_STEP_SUMMARY`, ensuring it ends with a newline. */
  def writeSummary(text: String): Unit =
    Env.opt(EnvNames.GitHubStepSummary).foreach: path =>
      val payload = if text.endsWith("\n") then text else text + "\n"
      os.write.append(Env.toAbsolutePath(path), payload)
