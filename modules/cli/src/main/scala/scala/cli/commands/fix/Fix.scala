package scala.cli.commands.fix

import caseapp.core.RemainingArgs

import java.nio.charset.StandardCharsets

import scala.build.input.ElementsUtils.projectSettingsFiles
import scala.build.input.Inputs
import scala.build.internal.{Constants, CustomCodeWrapper}
import scala.build.preprocessing.PreprocessedSource._
import scala.build.preprocessing.{DirectivesPositions, PreprocessedSource, ScopePath}
import scala.build.{CrossSources, Logger, Position, Sources}
import scala.cli.commands.shared.SharedOptions
import scala.cli.commands.{ScalaCommand, SpecificationLevel}

object Fix extends ScalaCommand[FixOptions] {
  override def group                   = "Main"
  override def scalaSpecificationLevel = SpecificationLevel.RESTRICTED
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
    val (_, preprocessedSources) = CrossSources.allInputsAndPreprocessedSources(
      inputs,
      preprocessors,
      options.shared.logger
    ).orExit(logger)

    val projectFilePath = inputs.elements.projectSettingsFiles.headOption match
      case Some(s) => s.path
      case _       => inputs.workspace / Constants.projectFileName
    val preprocessedWithUsingDirs = preprocessedSources.filter(_.directivesPositions.isDefined)

    preprocessedWithUsingDirs.length match
      case 0 => logger.message("No using directives have been found")
      case 1 => logger.message("All using directives are already in one file")
      case n =>
        logger.message(s"Using directives found in $n files. Migrating...")
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
        val allFoundDirectives = directivesSeq.fold(Nil)(_ ++ _).mkString("\n")

        preprocessedWithUsingDirs.find(_.scopePath == ScopePath.fromPath(projectFilePath)) match
          case Some(projectFile) =>
            logger.message(s"Found existing project file at $projectFilePath")
            val newProjectFileContent = projectFile.directivesPositions match
              case Some(positions) =>
                val (projectFileCode, projectFileDirectives) =
                  splitCodeAndDirectives(projectFile, positions)
                projectFileDirectives + "\n" + allFoundDirectives + "\n" + projectFileCode
              case _ =>
                allFoundDirectives + "\n" + os.read(projectFilePath)

            logger.message(s"Moving all directives to $projectFilePath")
            os.write.over(projectFilePath, newProjectFileContent.getBytes(StandardCharsets.UTF_8))

          case _ =>
            logger.message(s"Creating project file at $projectFilePath")
            logger.message(s"Moving all directives to $projectFilePath")
            os.write(
              projectFilePath,
              allFoundDirectives.getBytes(StandardCharsets.UTF_8),
              createFolders = true
            )
        logger.message(
          s"Successfully moved all using directives to the project file: $projectFilePath"
        )

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
}
