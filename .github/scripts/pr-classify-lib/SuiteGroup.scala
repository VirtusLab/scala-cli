package prclassify

/** High-level suite groups reported in the PR classification comment. Each entry corresponds to one
  * of the distinct `SHOULD_RUN: ...` expressions used across the jobs in
  * `.github/workflows/ci.yml`.
  */
enum SuiteGroup(val label: String):
  case UnitAndMill          extends SuiteGroup("Unit tests & fish shell")
  case JvmIntegration       extends SuiteGroup("JVM integration tests")
  case NativeIntegration    extends SuiteGroup("Native integration tests")
  case DocsTests            extends SuiteGroup("Docs tests")
  case Checks               extends SuiteGroup("Checks")
  case Format               extends SuiteGroup("Format / scalafix")
  case ReferenceDoc         extends SuiteGroup("Reference docs")
  case BloopMemoryFootprint extends SuiteGroup("Bloop memory footprint")
  case SbtExportVcRedist    extends SuiteGroup("Sbt export / vc-redist")

object SuiteGroup:

  /** Display order used in the generated comment. */
  val ordered: Seq[SuiteGroup] = values.toIndexedSeq
