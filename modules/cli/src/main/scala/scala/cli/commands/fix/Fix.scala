package scala.cli.commands.fix

import caseapp.core.RemainingArgs

import java.nio.charset.StandardCharsets

import scala.build.CrossSources._
import scala.build.input.ElementsUtils.{mainProjectSettingsFiles, testProjectSettingsFiles}
import scala.build.input.Inputs
import scala.build.internal.{Constants, CustomCodeWrapper}
import scala.build.preprocessing.PreprocessedSource._
import scala.build.preprocessing.{DirectivesPositions, PreprocessedSource, ScopePath}
import scala.build.{Logger, Position, Sources}
import scala.cli.commands.shared.SharedOptions
import scala.cli.commands.{ScalaCommand, SpecificationLevel}
import scala.io.Source

object Fix extends ScalaCommand[FixOptions] {
  override def group                   = "Main"
  override def scalaSpecificationLevel = SpecificationLevel.EXPERIMENTAL
  override def sharedOptions(options: FixOptions): Option[SharedOptions] = Some(options.shared)

  override def runCommand(options: FixOptions, args: RemainingArgs, logger: Logger): Unit = {
    if (options.migrateDirectives.contains(true)) runMigrateDirectives(options, args, logger)
  }

  private def runMigrateDirectives(
    options: FixOptions,
    args: RemainingArgs,
    logger: Logger
  ): Unit = {
    val buildOptions = buildOptionsOrExit(options)
    val inputs       = options.shared.inputs(args.remaining, () => Inputs.default()).orExit(logger)
    val preprocessors = Sources.defaultPreprocessors(
      buildOptions.scriptOptions.codeWrapper.getOrElse(CustomCodeWrapper),
      buildOptions.archiveCache,
      buildOptions.internal.javaClassNameVersionOpt,
      () => buildOptions.javaHome().value.javaCommand
    )
    val (_, allPreprocessedSources) = allInputsAndPreprocessedSources(
      inputs,
      preprocessors,
      options.shared.logger
    ).orExit(logger)

    (options.mainScope, options.testScope) match
      case (None, None) | (Some(true), Some(true)) =>
        migrateDirectivesForMainScope()
        logger.message("\n")
        migrateDirectivesForTestScope()
      case (Some(true), _) =>
        migrateDirectivesForMainScope()
      case (_, Some(true)) =>
        migrateDirectivesForTestScope()
      case _ => ()

    def migrateDirectives(
      preprocessedSources: Seq[PreprocessedSource],
      projectFilePath: os.Path
    ) = {
      val preprocessedWithUsingDirs = preprocessedSources.filter(_.directivesPositions.isDefined)
      preprocessedWithUsingDirs.length match
        case 0 => logger.message("No using directives have been found")
        case 1 => logger.message("All using directives are already in one file")
        case n =>
          logger.message(s"Using directives found in $n files. Migrating...")
          preprocessedSources.find(_.scopePath == ScopePath.fromPath(projectFilePath)) match
            case Some(projectFile) =>
              logger.message(s"Found existing project file at $projectFilePath")
              logger.message(s"Moving all directives to $projectFilePath")
              val newContent = newProjectFileContent(projectFile, allFoundDirectives)
              os.write.over(
                projectFilePath,
                newContent.getBytes(StandardCharsets.UTF_8)
              )
            case _ =>
              logger.message(s"Creating project file at $projectFilePath")
              logger.message(s"Moving all directives to $projectFilePath")
              val newContent = removeEmptyLines(allFoundDirectives)
              os.write(
                projectFilePath,
                newContent.getBytes(StandardCharsets.UTF_8),
                createFolders = true
              )
          logger.message(
            s"Successfully moved all using directives to the project file: $projectFilePath"
          )

      def allFoundDirectives = {
        val directivesSeq = preprocessedWithUsingDirs
          .filter(_.scopePath != ScopePath.fromPath(projectFilePath))
          .map { source =>
            source match
              case file: (OnDisk | InMemory) =>
                val result = file.directivesPositions match
                  case Some(positions) =>
                    val (code, directives) = splitCodeAndDirectives(file, positions)
                    file match
                      case f: OnDisk =>
                        logger.message(s"Deleting directives from ${f.path}...")
                        os.write.over(f.path, code.getBytes(StandardCharsets.UTF_8))
                      case f: InMemory =>
                        logger.message(s"Moving directives from ${f.relPath}...")
                    Seq(directives)
                  case _ => Nil
                result
              case _ => Nil
          }
        directivesSeq.fold(Nil)(_ ++ _).mkString("\n")
      }

      def newProjectFileContent(projectFile: PreprocessedSource, additionalDirectives: String) = {
        val (newCode, newDirectives) = projectFile.directivesPositions match
          case Some(positions) =>
            val (code, directives) = splitCodeAndDirectives(projectFile, positions)
            (code, removeEmptyLines(directives + "\n" + additionalDirectives))
          case _ =>
            (os.read(projectFilePath), removeEmptyLines(additionalDirectives))
        newCode match
          case "" => newDirectives + "\n"
          case _  => newDirectives + "\n\n" + newCode
      }

      def splitCodeAndDirectives(
        file: PreprocessedSource,
        positions: DirectivesPositions
      ): (String, String) = {
        val fileContent = file match
          case f: OnDisk   => os.read(f.path)
          case f: InMemory => f.code
          case _           => ""

        val lineStartIndices     = Position.Raw.lineStartIndices(fileContent)
        val (endLine, endColumn) = (positions.scope.endPos._1, positions.scope.endPos._2)
        val endIndex             = lineStartIndices(endLine) + endColumn

        (fileContent.drop(endIndex), fileContent.take(endIndex))
      }
    }

    def removeEmptyLines(input: String): String =
      Source
        .fromBytes(input.getBytes(StandardCharsets.UTF_8))
        .getLines()
        .filterNot(_.isEmpty())
        .mkString("\n")

    def migrateDirectivesForMainScope() = {
      logger.message("Migrating main scope directives")
      val mainPreprocessedSources =
        allPreprocessedSources.filterNot(s => isTestSource(s.scopePath, inputs))
      val mainProjectFilePath = inputs.elements.mainProjectSettingsFiles.headOption match
        case Some(s) => s.path
        case _       => inputs.workspace / Constants.mainProjectFileName

      migrateDirectives(mainPreprocessedSources, mainProjectFilePath)
    }

    def migrateDirectivesForTestScope() = {
      logger.message("Migrating test scope directives")
      val testPreprocessedSources =
        allPreprocessedSources.filter(s => isTestSource(s.scopePath, inputs))
      val testProjectFilePath = inputs.elements.testProjectSettingsFiles.headOption match
        case Some(s) => s.path
        case _       => inputs.workspace / Constants.testProjectFileName

      migrateDirectives(testPreprocessedSources, testProjectFilePath)
    }
  }
}
