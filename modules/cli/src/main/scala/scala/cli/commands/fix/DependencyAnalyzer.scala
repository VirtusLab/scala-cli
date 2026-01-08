package scala.cli.commands.fix

import dependency.AnyDependency

import scala.build.options.BuildOptions
import scala.build.{Artifacts, Logger, Positioned, Sources}
import scala.collection.mutable
import scala.util.Try
import scala.util.matching.Regex

/** Analyzer for detecting unused and missing explicit dependencies in a Scala project.
  *
  * This object provides functionality similar to tools like sbt-explicit-dependencies and mill-explicit-deps:
  * - Detects compile-time dependencies that are declared but not used
  * - Detects transitive dependencies that are directly imported but not explicitly declared
  *
  * The analysis is based on static analysis of import statements and may produce false positives for
  * dependencies used via reflection, service loading, or other dynamic mechanisms.
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
    usedInFiles: Seq[os.Path],
    reason: String
  )

  // Regex to extract imports from Scala code
  private val importPattern: Regex = """^\s*import\s+([^\s{(]+).*""".r

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

    // Extract all imports from source files
    val allImports = extractImports(sources, logger)
    logger.debug(s"Found ${allImports.size} unique import statements")

    // Get declared dependencies
    val declaredDeps = buildOptions.classPathOptions.extraDependencies.toSeq

    // Get transitive dependencies from resolution
    val resolutionOpt = artifacts.resolution
    
    if (resolutionOpt.isEmpty)
      Left("No dependency resolution available. Please compile the project first.")
    else {
      val resolution = resolutionOpt.get

      // Analyze unused dependencies
      val unusedDeps = detectUnusedDependencies(
        declaredDeps,
        allImports,
        logger
      )

      // Analyze missing explicit dependencies
      val missingDeps = detectMissingExplicitDependencies(
        declaredDeps,
        allImports,
        resolution,
        sources,
        logger
      )

      Right(DependencyAnalysisResult(unusedDeps, missingDeps))
    }
  }

  /** Extracts all import statements from source files.
    *
    * @param sources
    *   project source files
    * @param logger
    *   logger for error messages
    * @return
    *   set of unique import package names
    */
  private def extractImports(sources: Sources, logger: Logger): Set[String] = {
    val imports = mutable.Set[String]()

    // Extract from path-based sources
    sources.paths.foreach { case (path, _) =>
      Try {
        val content = os.read(path)
        content.linesIterator.foreach {
          case importPattern(importPath) =>
            // Extract the base package (first few segments)
            imports += importPath.trim
          case _ => ()
        }
      }.recover { case ex =>
        logger.debug(s"Failed to extract imports from $path: ${ex.getMessage}")
      }
    }

    // Extract from in-memory sources
    sources.inMemory.foreach { inMem =>
      Try {
        val content = new String(inMem.content)
        content.linesIterator.foreach {
          case importPattern(importPath) =>
            imports += importPath.trim
          case _ => ()
        }
      }.recover { case ex =>
        logger.debug(s"Failed to extract imports from in-memory source: ${ex.getMessage}")
      }
    }

    imports.toSet
  }

  /** Detects dependencies that are declared but appear to be unused.
    *
    * A dependency is considered unused if none of its package names are imported in the source code.
    *
    * @param declaredDeps
    *   explicitly declared dependencies
    * @param imports
    *   set of imported packages
    * @param artifacts
    *   resolved artifacts
    * @param logger
    *   logger for debug output
    * @return
    *   list of potentially unused dependencies
    */
  private def detectUnusedDependencies(
    declaredDeps: Seq[Positioned[AnyDependency]],
    imports: Set[String],
    logger: Logger
  ): Seq[UnusedDependency] = {
    
    // Map dependencies to their group/artifact identifiers
    val depToArtifactMap = declaredDeps.map { posDep =>
      val dep = posDep.value
      (dep, s"${dep.organization}.${dep.name}")
    }

    // For each dependency, check if its packages are imported
    val unused = depToArtifactMap.flatMap { case (dep, _) =>
      // Common package name patterns from artifact names and organizations
      val possiblePackages = Set(
        dep.organization.replace('-', '.').toLowerCase,
        dep.name.replace('-', '.').toLowerCase,
        s"${dep.organization}.${dep.name}".replace('-', '.').toLowerCase
      )

      // Check if any import starts with potential package names
      val isUsed = imports.exists { imp =>
        val impLower = imp.toLowerCase
        possiblePackages.exists(pkg => impLower.startsWith(pkg))
      }

      if (!isUsed) {
        Some(UnusedDependency(
          dep,
          s"No imports found that could be provided by this dependency"
        ))
      } else {
        None
      }
    }

    logger.debug(s"Found ${unused.size} potentially unused dependencies")
    unused
  }

  /** Detects transitive dependencies that are directly imported but not explicitly declared.
    *
    * These dependencies are currently available transitively through other dependencies, but should be
    * declared explicitly to ensure build stability if upstream dependencies change.
    *
    * @param declaredDeps
    *   explicitly declared dependencies
    * @param imports
    *   set of imported packages
    * @param resolution
    *   dependency resolution graph
    * @param artifacts
    *   resolved artifacts
    * @param sources
    *   project source files
    * @param logger
    *   logger for debug output
    * @return
    *   list of missing explicit dependencies
    */
  private def detectMissingExplicitDependencies(
    declaredDeps: Seq[Positioned[AnyDependency]],
    imports: Set[String],
    resolution: coursier.Resolution,
    sources: Sources,
    logger: Logger
  ): Seq[MissingDependency] = {
    
    // Get all transitive dependencies from resolution
    val allDeps = resolution.dependencies.toSet
    
    // Get declared dependency modules
    val declaredModules = declaredDeps.map(_.value).map { dep =>
      (coursier.core.Organization(dep.organization), coursier.core.ModuleName(dep.name))
    }.toSet

    // Find transitive dependencies that are not explicitly declared
    val transitiveDeps = allDeps.filterNot { dep =>
      declaredModules.contains((dep.module.organization, dep.module.name))
    }

    logger.debug(s"Analyzing ${transitiveDeps.size} transitive dependencies")

    // For each transitive dependency, check if it's directly imported
    val missing = transitiveDeps.flatMap { dep =>
      val org = dep.module.organization.value
      val name = dep.module.name.value
      val version = dep.versionConstraint.asString

      // Possible package names from org and module name
      val possiblePackages = Set(
        org.replace('-', '.').toLowerCase,
        name.replace('-', '.').toLowerCase,
        s"$org.$name".replace('-', '.').toLowerCase
      )

      // Check if any import could be from this transitive dependency
      val matchingImports = imports.filter { imp =>
        val impLower = imp.toLowerCase
        possiblePackages.exists(pkg => impLower.startsWith(pkg))
      }

      if (matchingImports.nonEmpty) {
        // Find which files use this import
        val usedInFiles = findFilesWithImports(sources, matchingImports, logger)
        
        Some(MissingDependency(
          s"$org:$name",
          version,
          usedInFiles,
          s"Directly imported but not explicitly declared (transitive through other dependencies)"
        ))
      } else {
        None
      }
    }

    logger.debug(s"Found ${missing.size} potentially missing explicit dependencies")
    missing.toSeq
  }

  /** Finds which source files import the given packages.
    *
    * @param sources
    *   project source files
    * @param targetImports
    *   set of package imports to search for
    * @param logger
    *   logger for error messages
    * @return
    *   list of source files that import these packages
    */
  private def findFilesWithImports(
    sources: Sources,
    targetImports: Set[String],
    logger: Logger
  ): Seq[os.Path] = {
    val matchingFiles = mutable.Set[os.Path]()

    sources.paths.foreach { case (path, _) =>
      Try {
        val content = os.read(path)
        if (targetImports.exists(imp => content.contains(s"import $imp"))) {
          matchingFiles += path
        }
      }.recover { case ex =>
        logger.debug(s"Error reading file $path: ${ex.getMessage}")
      }
    }

    matchingFiles.toSeq
  }
}
