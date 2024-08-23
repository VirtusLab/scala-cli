package scala.build.compose

import toml.Value
import toml.Value.*

import scala.build.EitherCps.*
import scala.build.EitherSequence
import scala.build.bsp.buildtargets.ProjectName
import scala.build.compose.{ComposedInputs, Inputs, InputsComposer, SimpleInputs}
import scala.build.errors.{BuildException, CompositeBuildException, ModuleConfigurationError}
import scala.build.input.Module
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

  private[compose] object Keys {
    val modules   = "modules"
    val roots     = "roots"
    val dependsOn = "dependsOn"
  }

  private[compose] case class ModuleDefinition(
    name: String,
    roots: Seq[String],
    dependsOn: Seq[String] = Nil
  )

  // TODO Check for module dependencies that do not exist
  private[compose] def readAllModules(modules: Option[Value])
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

        val maybeDependsOn = values.get(Keys.dependsOn).map {
          case Arr(values) =>
            EitherSequence.sequence {
              values.map {
                case Str(value) => Right(value)
                case _          => Left(())
              }
            }.left.map(_ => ())
          case _ => Left(())
        }.getOrElse(Right(Nil))
          .left.map(_ =>
            ModuleConfigurationError(
              s"${Keys.modules}.$key.${Keys.dependsOn} must be a list of strings"
            )
          )

        for {
          roots     <- maybeRoots
          dependsOn <- maybeDependsOn
        } yield ModuleDefinition(key, roots, dependsOn)

      case _ => Left(ModuleConfigurationError(s"${Keys.modules}.$key must be a table"))
}

/** Creates [[Module]] given the initial arguments passed to the command, Looks for module config
  * .toml file and if found composes module inputs according to the defined config, otherwise if
  * module config is not found or if [[allowForbiddenFeatures]] is not set, returns only one basic
  * module created from initial args (see [[simpleInputs]])
  *
  * @param args
  *   initial args passed to command
  * @param cwd
  *   working directory
  * @param inputsFromArgs
  *   function that proceeds with the whole [[Module]] creation flow (validating elements, etc.)
  *   this takes into account options passed from CLI like in SharedOptions
  * @param allowForbiddenFeatures
  */
final case class InputsComposer(
  args: Seq[String],
  cwd: os.Path,
  inputsFromArgs: (Seq[String], Option[ProjectName]) => Either[BuildException, Module],
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
            _            <- checkForCycles(modules)
            moduleInputs <- fromModuleDefinitions(modules, moduleConfigPath)
          } yield moduleInputs
        case Right(None) => simpleInputs
        case Left(err)   => Left(err)
      }
    else simpleInputs

//  private def readScalaVersion(value: Value): Either[String, String] = value match {
//    case Str(version) => Right(version)
//    case _ => Left("scalaVersion must be a string")
//  }

  private def checkForCycles(modules: Seq[ModuleDefinition])
    : Either[ModuleConfigurationError, Unit] = either {
    val lookup   = Map.from(modules.map(module => module.name -> module))
    val seen     = mutable.Set.empty[String]
    val visiting = mutable.Set.empty[String]

    def visit(node: ModuleDefinition, from: ModuleDefinition | Null): Unit =
      if visiting.contains(node.name) then
        val fromName  = Option(from).map(_.name).getOrElse("<unknown>")
        val onMessage = if fromName == node.name then "itself." else s"module '${node.name}'."
        failure(ModuleConfigurationError(
          s"module graph is invalid: module '$fromName' has a cyclic dependency on $onMessage"
        ))
      else if !seen.contains(node.name) then
        visiting.add(node.name)
        for dep <- node.dependsOn do
          lookup.get(dep) match
            case Some(module) => visit(module, node)
            case _ => failure(ModuleConfigurationError(
                s"module '${node.name}' depends on '$dep' which does not exist."
              )) // TODO handle in module parsing for better error
        visiting.remove(node.name)
        seen.addOne(node.name)
      else ()
    end visit

    Right(lookup.values.foreach(visit(_, null)))
  }

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
    val moduleInputsInfo: Map[ModuleDefinition, Module] = modules.map { m =>
      val moduleName        = ProjectName(m.name)
      val argsWithWorkspace = m.roots.map(r => os.Path(r, workspacePath).toString)
      val moduleInputs      = inputsFromArgs(argsWithWorkspace, Some(moduleName))
      m -> value(moduleInputs).copy(mayAppendHash = false)
    }.toMap

    val projectNameMap: Map[String, ProjectName] =
      moduleInputsInfo.map((moduleDef, module) => moduleDef.name -> module.projectName)

    val moduleInputs = moduleInputsInfo.map { (moduleDef, module) =>
      val moduleDeps: Seq[ProjectName] = moduleDef.dependsOn.map(projectNameMap)

      module.dependsOn(moduleDeps)
        .withForcedWorkspace(workspacePath)
        .copy(mayAppendHash = false)
    }.toSeq

    val targetModule = modules.find(_.roots.toSet equals args.toSet)
      .map(moduleInputsInfo)
      .getOrElse(moduleInputs.head)

    ComposedInputs(modules = moduleInputs, targetModule = targetModule, workspace = workspacePath)
  }
}
