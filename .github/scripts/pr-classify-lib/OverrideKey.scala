package prclassify

/** Override markers users can add to a PR body to force certain suite groups on even when the diff
  * wouldn't normally trigger them (e.g. `[test_native]`).
  */
enum OverrideKey(val keyword: String):
  case TestAll         extends OverrideKey("test_all")
  case TestNative      extends OverrideKey("test_native")
  case TestIntegration extends OverrideKey("test_integration")
  case TestDocs        extends OverrideKey("test_docs")
  case TestFormat      extends OverrideKey("test_format")

  /** The literal marker users write in PR bodies, e.g. "[test_native]". */
  def marker: String = s"[$keyword]"

object OverrideKey:

  /** Display order used in outputs and summaries. */
  val ordered: Seq[OverrideKey] = values.toIndexedSeq

  def fromKeyword(keyword: String): Option[OverrideKey] =
    values.find(_.keyword == keyword)
