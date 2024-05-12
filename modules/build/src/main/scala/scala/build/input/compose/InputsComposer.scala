package scala.build.input.compose

import toml.Value
import toml.Value.*

import scala.build.EitherCps.*
import scala.build.EitherSequence
import scala.build.bsp.buildtargets.ProjectName
import scala.build.errors.{BuildException, CompositeBuildException, ModuleConfigurationError}
import scala.build.input.ModuleInputs
import scala.build.input.compose.InputsComposer
import scala.build.internal.Constants
import scala.build.options.BuildOptions
import scala.collection.mutable

object InputsComposer {

  // TODO errors on corner cases
  def findModuleConfig(
    args: Seq[String],
    cwd: os.Path
  ): Either[ModuleConfigurationError, Option[os.Path]] = {
    def moduleConfigDirectlyFromArgs = {
      val moduleConfigPathOpt = args
        .map(arg => os.Path(arg, cwd))
        .find(_.endsWith(os.RelPath(Constants.moduleConfigFileName)))

      moduleConfigPathOpt match {
        case Some(path) if os.exists(path) => Right(Some(path))
        case Some(path) => Left(ModuleConfigurationError(
            s"""File does not exist:
               | - $path
               |""".stripMargin
          ))
        case None => Right(None)
      }
    }

    def moduleConfigFromCwd =
      Right(os.walk(cwd).find(p => p.endsWith(os.RelPath(Constants.moduleConfigFileName))))

    for {
      fromArgs <- moduleConfigDirectlyFromArgs
      fromCwd  <- moduleConfigFromCwd
    } yield fromArgs.orElse(fromCwd)
  }

  private[input] object Keys {
    val modules = "modules"
    val roots   = "roots"
  }

  private[input] case class ModuleDefinition(
    name: String,
    roots: Seq[String]
  )

  // TODO Check for module dependencies that do not exist
  private[input] def readAllModules(modules: Option[Value])
    : Either[BuildException, Seq[ModuleDefinition]] = modules match {
    case Some(Tbl(values)) => EitherSequence.sequence {
        values.toSeq.map(readModule)
      }.left.map(CompositeBuildException.apply)
    case _ => Left(ModuleConfigurationError(s"$modules must exist and must be a table"))
  }

  private def readModule(
    key: String,
    value: Value
  ): Either[ModuleConfigurationError, ModuleDefinition] =
    value match
      case Tbl(values) =>
        val maybeRoots = values.get(Keys.roots).map {
          case Str(value) => Right(Seq(value))
          case Arr(values) => EitherSequence.sequence {
              values.map {
                case Str(value) => Right(value)
                case _          => Left(())
              }
            }.left.map(_ => ())
          case _ => Left(())
        }.getOrElse(Right(Seq(key)))
          .left.map(_ =>
            ModuleConfigurationError(
              s"${Keys.modules}.$key.${Keys.roots} must be a string or a list of strings"
            )
          )

        for {
          roots <- maybeRoots
        } yield ModuleDefinition(key, roots)

      case _ => Left(ModuleConfigurationError(s"${Keys.modules}.$key must be a table"))
}

/** Creates [[ModuleInputs]] given the initial arguments passed to the command, Looks for module
  * config .toml file and if found composes module inputs according to the defined config, otherwise
  * if module config is not found or if [[allowForbiddenFeatures]] is not set, returns only one
  * basic module created from initial args (see [[simpleInputs]])
  *
  * @param args
  *   initial args passed to command
  * @param cwd
  *   working directory
  * @param inputsFromArgs
  *   function that proceeds with the whole [[ModuleInputs]] creation flow (validating elements,
  *   etc.) this takes into account options passed from CLI like in SharedOptions
  * @param allowForbiddenFeatures
  */
final case class InputsComposer(
  args: Seq[String],
  cwd: os.Path,
  inputsFromArgs: (Seq[String], Option[ProjectName]) => Either[BuildException, ModuleInputs],
  allowForbiddenFeatures: Boolean
) {
  import InputsComposer.*

  /** Inputs with no dependencies coming only from args */
  private def simpleInputs = for (inputs <- inputsFromArgs(args, None)) yield SimpleInputs(inputs)

  def getInputs: Either[BuildException, Inputs] =
    if allowForbiddenFeatures then
      findModuleConfig(args, cwd) match {
        case Right(Some(moduleConfigPath)) =>
          val configText = os.read(moduleConfigPath)
          for {
            table <-
              toml.Toml.parse(configText).left.map(e =>
                ModuleConfigurationError(e._2)
              ) // TODO use the Address value returned to show better errors
            modules      <- readAllModules(table.values.get(Keys.modules))
            moduleInputs <- fromModuleDefinitions(modules, moduleConfigPath)
          } yield moduleInputs
        case Right(None) => simpleInputs
        case Left(err)   => Left(err)
      }
    else simpleInputs

  /** Create module inputs using a supplied function [[inputsFromArgs]], link them with their module
    * dependencies' names
    *
    * @return
    *   a list of module inputs for the extracted modules
    */
  private def fromModuleDefinitions(
    modules: Seq[ModuleDefinition],
    moduleConfigPath: os.Path
  ): Either[BuildException, ComposedInputs] = either {
    val workspacePath = moduleConfigPath / os.up
    val moduleInputs: Seq[ModuleInputs] = modules.map { m =>
      val moduleName        = ProjectName(m.name)
      val argsWithWorkspace = m.roots.map(r => os.Path(r, workspacePath).toString)
      val moduleInputs      = inputsFromArgs(argsWithWorkspace, Some(moduleName))
      value(moduleInputs)
        .copy(mayAppendHash =
          false
        ) // Important only for modules with dependencies in bloop config, but let's keep it
        .withForcedWorkspace(workspacePath)
    }

    ComposedInputs(modules = moduleInputs, workspace = workspacePath)
  }
}
