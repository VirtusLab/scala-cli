package scala.cli.commands.fix

import dependency.AnyDependency

import scala.build.options.BuildOptions
import scala.build.{Artifacts, Logger, Positioned, Sources}
import scala.collection.mutable
import scala.util.Try
import scala.util.matching.Regex

/** Analyzer for detecting unused and missing explicit dependencies in a Scala project.
  *
  * This object provides functionality similar to tools like sbt-explicit-dependencies and
  * mill-explicit-deps:
  *   - Detects compile-time dependencies that are declared but not used
  *   - Detects transitive dependencies that are directly imported but not explicitly declared
  *
  * The analysis is based on static analysis of import statements and may produce false positives
  * for dependencies used via reflection, service loading, or other dynamic mechanisms.
  */
object DependencyAnalyzer {

  /** Result of analyzing project dependencies.
    *
    * @param unusedDependencies
    *   dependencies that are declared but not used
    * @param missingExplicitDependencies
    *   transitive dependencies that are directly imported but not explicitly declared
    */
  final case class DependencyAnalysisResult(
    unusedDependencies: Seq[UnusedDependency],
    missingExplicitDependencies: Seq[MissingDependency]
  )

  /** A dependency that appears to be unused.
    *
    * @param dependency
    *   the unused dependency
    * @param reason
    *   explanation of why it's considered unused
    */
  final case class UnusedDependency(
    dependency: AnyDependency,
    reason: String
  )

  /** A transitive dependency that is directly used but not explicitly declared.
    *
    * @param organizationModule
    *   organization and module name (e.g., "org.example:my-lib")
    * @param version
    *   version of the dependency
    * @param usedInFiles
    *   source files that import this dependency
    * @param reason
    *   explanation of why it should be declared
    */
  final case class MissingDependency(
    organizationModule: String,
    version: String,
    usedInFiles: Seq[String],
    reason: String
  )

  /** Container for parsed source file data.
    *
    * @param path
    *   path to the source file (or logic path for in-memory)
    * @param imports
    *   package names found in import statements
    * @param simpleUsages
    *   dotted identifiers found in the code (candidates for package usage)
    */
  private case class ParsedSource(
    path: String,
    imports: Set[String],
    simpleUsages: Set[String]
  )

  /** lightweight tokenizer to extract imports and usages while ignoring comments and string
    * literals.
    */
  private object Tokenizer {
    private sealed trait State
    private case object Code            extends State
    private case object StringLiteral   extends State
    private case object MultiLineString extends State
    private case object LineComment     extends State
    private case object BlockComment    extends State

    // Regex to extract imports from a code line (already stripped of strings/comments)
    private val importPattern: Regex = """^\s*import\s+([^\s{(]+).*""".r
    // Regex to extract simple package/object usages
    private val usagePattern: Regex =
      """\b([a-z][a-zA-Z0-9_]*+(?:\.[a-z][a-zA-Z0-9_]*+)*+)\b""".r

    def parse(content: String): (Set[String], Set[String]) = {
      val imports = mutable.Set[String]()
      val usages  = mutable.Set[String]()

      val sb           = new StringBuilder
      var state: State = Code
      var i            = 0
      val len          = content.length

      while (i < len) {
        val c = content(i)
        // Check for state transitions
        state match {
          case Code =>
            if (c == '"') {
              if (i + 2 < len && content(i + 1) == '"' && content(i + 2) == '"') {
                state = MultiLineString
                i += 2
              }
              else
                state = StringLiteral
              sb.append(' ') // replace string content with space to preserve token boundaries
            }
            else if (c == '/' && i + 1 < len && content(i + 1) == '/') {
              state = LineComment
              i += 1
              sb.append(' ')
            }
            else if (c == '/' && i + 1 < len && content(i + 1) == '*') {
              state = BlockComment
              i += 1
              sb.append(' ')
            }
            else
              sb.append(c)
          case StringLiteral =>
            if (c == '"' && (i == 0 || content(i - 1) != '\\')) {
              state = Code
              sb.append(' ')
            }
          // ignore content
          case MultiLineString =>
            if (c == '"' && i + 2 < len && content(i + 1) == '"' && content(i + 2) == '"') {
              state = Code
              i += 2
              sb.append(' ')
            }
          // ignore content
          case LineComment =>
            if (c == '\n' || c == '\r') {
              state = Code
              sb.append(c)
            }
          case BlockComment =>
            if (c == '*' && i + 1 < len && content(i + 1) == '/') {
              state = Code
              i += 1
              sb.append(' ')
            }
        }
        i += 1
      }

      // now analyse the code-only content
      val codeText = sb.toString()
      codeText.linesIterator.foreach { line =>
        val trimmed = line.trim
        if (trimmed.startsWith("import "))
          importPattern.findFirstMatchIn(trimmed).foreach { m =>
            imports += m.group(1)
          }
        else
          usagePattern.findAllIn(trimmed).matchData.foreach { m =>
            usages += m.group(1)
          }
      }

      (imports.toSet, usages.toSet)
    }
  }

  /** Analyzes dependencies in a project to find unused and missing explicit dependencies.
    *
    * @param sources
    *   project source files to analyze
    * @param buildOptions
    *   build configuration including declared dependencies
    * @param artifacts
    *   resolved artifacts including dependency resolution graph
    * @param logger
    *   logger for debug output
    * @return
    *   either an error message or a DependencyAnalysisResult
    */
  def analyzeDependencies(
    sources: Sources,
    buildOptions: BuildOptions,
    artifacts: Artifacts,
    logger: Logger
  ): Either[String, DependencyAnalysisResult] = {
    logger.debug("Starting dependency analysis...")

    // 1. Parse all sources once
    val parsedSources = parseAllSources(sources, logger)
    logger.debug(s"Parsed ${parsedSources.size} source files")

    // 2. Aggregate all imports and usages
    val allImports    = parsedSources.flatMap(_.imports).toSet
    val allUsages     = parsedSources.flatMap(_.simpleUsages).toSet
    val allReferences = allImports ++ allUsages

    logger.debug(s"Found ${allImports.size} unique imports and ${allUsages.size} simple usages")

    // Get declared dependencies
    val declaredDeps = buildOptions.classPathOptions.extraDependencies.toSeq

    // Get transitive dependencies from resolution
    val resolutionOpt = artifacts.resolution

    // Analyze unused dependencies
    val unusedDeps = detectUnusedDependencies(
      declaredDeps,
      allReferences,
      logger
    )

    // Analyze missing explicit dependencies
    val missingDeps = resolutionOpt match {
      case Some(resolution) =>
        detectMissingExplicitDependencies(
          declaredDeps,
          allReferences,
          resolution,
          parsedSources,
          logger
        )
      case None =>
        logger.debug("Skipping missing explicit dependency detection: no resolution available")
        Seq.empty
    }

    Right(DependencyAnalysisResult(unusedDeps, missingDeps))
  }

  private def parseAllSources(sources: Sources, logger: Logger): Seq[ParsedSource] = {
    val results = mutable.ListBuffer[ParsedSource]()

    // Parse path-based sources
    sources.paths.foreach { case (path, _) =>
      Try {
        val content           = os.read(path)
        val (imports, usages) = Tokenizer.parse(content)
        results += ParsedSource(path.toString, imports, usages)
      }.recover { case ex =>
        logger.debug(s"Failed to parse $path: ${ex.getMessage}")
      }
    }

    // Parse in-memory sources
    sources.inMemory.foreach { inMem =>
      Try {
        val content           = new String(inMem.content)
        val (imports, usages) = Tokenizer.parse(content)
        results += ParsedSource(
          inMem.originalPath.map(_.toString).getOrElse("in-memory"),
          imports,
          usages
        )
      }.recover { case ex =>
        logger.debug(s"Failed to parse in-memory source: ${ex.getMessage}")
      }
    }

    results.toSeq
  }

  /** Detects dependencies that are declared but appear to be unused.
    */
  private def detectUnusedDependencies(
    declaredDeps: Seq[Positioned[AnyDependency]],
    references: Set[String],
    logger: Logger
  ): Seq[UnusedDependency] = {

    val depToArtifactMap = declaredDeps.map { posDep =>
      val dep = posDep.value
      (dep, s"${dep.organization}.${dep.name}")
    }

    val unused = depToArtifactMap.flatMap { case (dep, _) =>
      val possiblePackages = Set(
        dep.organization.replace('-', '.').toLowerCase,
        dep.name.replace('-', '.').toLowerCase,
        s"${dep.organization}.${dep.name}".replace('-', '.').toLowerCase
      )

      val isUsed = references.exists { ref =>
        val refLower = ref.toLowerCase
        possiblePackages.exists(pkg => refLower.startsWith(pkg))
      }

      if (!isUsed)
        Some(UnusedDependency(
          dep,
          s"No imports or usages found that could be provided by this dependency"
        ))
      else
        None
    }

    logger.debug(s"Found ${unused.size} potentially unused dependencies")
    unused
  }

  /** Detects transitive dependencies that are directly imported but not explicitly declared.
    */
  private def detectMissingExplicitDependencies(
    declaredDeps: Seq[Positioned[AnyDependency]],
    references: Set[String],
    resolution: coursier.Resolution,
    parsedSources: Seq[ParsedSource],
    logger: Logger
  ): Seq[MissingDependency] = {

    val allDeps = resolution.dependencies.toSet

    val declaredModules = declaredDeps.map(_.value).map { dep =>
      (coursier.core.Organization(dep.organization), coursier.core.ModuleName(dep.name))
    }.toSet

    val transitiveDeps = allDeps.filterNot { dep =>
      declaredModules.contains((dep.module.organization, dep.module.name))
    }

    val missing = transitiveDeps.flatMap { dep =>
      val org     = dep.module.organization.value
      val name    = dep.module.name.value
      val version = dep.versionConstraint.asString

      val simpleName = name.replaceAll("_\\d+(\\.\\d+)*$", "")

      // Possible package names from org and module name
      val possiblePackages = Set(
        org.replace('-', '.').toLowerCase,
        name.replace('-', '.').toLowerCase,
        simpleName.replace('-', '.').toLowerCase,
        s"$org.$name".replace('-', '.').toLowerCase,
        s"$org.$simpleName".replace('-', '.').toLowerCase
      )

      val matchingReferences = references.filter { ref =>
        val refLower = ref.toLowerCase
        possiblePackages.exists(pkg => refLower.startsWith(pkg))
      }

      if (matchingReferences.nonEmpty) {
        // Find which files use these references using cached parsed data
        val usedInFiles = parsedSources.collect {
          case ps
              if ps.imports.exists(matchingReferences.contains) || ps.simpleUsages.exists(
                matchingReferences.contains
              ) =>
            ps.path
        }

        Some(MissingDependency(
          s"$org:$name",
          version,
          usedInFiles,
          s"Directly used but not explicitly declared (transitive through other dependencies)"
        ))
      }
      else
        None
    }

    logger.debug(s"Found ${missing.size} potentially missing explicit dependencies")
    missing.toSeq
  }
  def reportUnusedDependencies(
    unusedDeps: Seq[UnusedDependency],
    logger: Logger
  ): Unit = {
    if (unusedDeps.isEmpty)
      logger.message("✓ No unused dependencies found.")
    else {
      logger.message(s"\n⚠ Found ${unusedDeps.length} potentially unused dependencies:\n")
      unusedDeps.foreach { unused =>
        val dep = unused.dependency
        logger.message(s"  • ${dep.organization}:${dep.name}:${dep.version}")
        logger.message(s"    ${unused.reason}")
        logger.message(s"    Consider removing: //> using dep ${dep.render}\n")
      }
      logger.message(
        "Note: This analysis is based on import statements and may produce false positives."
      )
      logger.message(
        "Dependencies might be used via reflection, service loading, or other mechanisms.\n"
      )
    }
  }

  def reportMissingDependencies(
    missingDeps: Seq[MissingDependency],
    logger: Logger
  ): Unit = {
    if (missingDeps.isEmpty)
      logger.message("✓ All directly used dependencies are explicitly declared.")
    else {
      logger.message(
        s"\n⚠ Found ${missingDeps.length} transitive dependencies that are directly used:\n"
      )
      missingDeps.foreach { missing =>
        logger.message(s"  • ${missing.organizationModule}:${missing.version}")
        logger.message(s"    ${missing.reason}")
        if (missing.usedInFiles.nonEmpty)
          logger.message(s"    Used in: ${missing.usedInFiles.map(p =>
              java.nio.file.Paths.get(p).getFileName.toString
            ).mkString(", ")}")
        logger.message(
          s"    Consider adding: //> using dep ${missing.organizationModule}:${missing.version}\n"
        )
      }
      logger.message(
        "Note: These dependencies are currently available transitively but should be declared explicitly."
      )
      logger.message("This ensures your build remains stable if upstream dependencies change.\n")
    }
  }
}
