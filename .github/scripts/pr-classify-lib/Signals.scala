package prclassify

/** A pair of (which change categories fired, which overrides are active). This captures everything
  * needed to decide which suite groups will run.
  */
case class Signals(categories: Set[Category], overrides: Set[OverrideKey]):

  def has(c: Category): Boolean    = categories.contains(c)
  def has(o: OverrideKey): Boolean = overrides.contains(o)

  def withoutOverrides: Signals = copy(overrides = Set.empty)

  def withOverride(key: OverrideKey, enabled: Boolean): Signals =
    copy(overrides = if enabled then overrides + key else overrides - key)

  /** Evaluates the SHOULD_RUN boolean for the given suite group. Keep these predicates in sync with
    * the `SHOULD_RUN: ...` expressions used in `.github/workflows/ci.yml` — see the file-line
    * references next to each branch for where to look.
    */
  def shouldRun(group: SuiteGroup): Boolean =
    import Category.*
    import OverrideKey.*
    import SuiteGroup.*
    group match
      // unit-tests (ci.yml line 54), test-fish-shell (line 109)
      case UnitAndMill =>
        has(Code) || has(Ci) || has(MillWrapper) || has(TestAll)
      // jvm-*-tests-* (ci.yml lines 143, 181, 219, 257, 295, 333)
      case JvmIntegration =>
        has(Code) || has(Ci) || has(TestAll) || has(TestIntegration)
      // native-*-tests-*, generate-*-launcher (ci.yml lines 371 .. 1601)
      case NativeIntegration =>
        has(Code) || has(Ci) || has(TestAll) || has(TestNative)
      // docs-tests (ci.yml line 1650)
      case DocsTests =>
        has(Code) || has(Docs) || has(Ci) || has(Gifs) || has(TestAll) || has(TestDocs)
      // checks (ci.yml line 1696)
      case Checks =>
        has(Code) || has(Docs) || has(Ci) || has(FormatConfig) || has(TestAll)
      // format (ci.yml line 1740)
      case Format =>
        has(Code) || has(Docs) || has(Ci) || has(FormatConfig) || has(TestAll) || has(TestFormat)
      // reference-doc (ci.yml line 1764)
      case ReferenceDoc =>
        has(Code) || has(Docs) || has(Ci) || has(TestAll)
      // bloop-memory-footprint (ci.yml line 1793)
      case BloopMemoryFootprint =>
        has(Code) || has(Ci) || has(Benchmark) || has(TestAll)
      // test-hypothetical-sbt-export, vc-redist (ci.yml lines 1832, 1860)
      case SbtExportVcRedist =>
        has(Code) || has(Ci) || has(TestAll)

object Signals:

  val empty: Signals = Signals(Set.empty, Set.empty)

  /** Reads KEY=VALUE maps produced by `classify-changes.sc` and `check-override-keywords.sc` and
    * turns them into a `Signals`.
    */
  def fromKeyValueMaps(
    categoryMap: Map[String, String],
    overrideMap: Map[String, String]
  ): Signals =
    def isTrue(v: String): Boolean = v.equalsIgnoreCase("true")
    Signals(
      categories = Category.values.iterator
        .filter(c => categoryMap.get(c.key).exists(isTrue))
        .toSet,
      overrides = OverrideKey.values.iterator
        .filter(o => overrideMap.get(o.keyword).exists(isTrue))
        .toSet
    )
