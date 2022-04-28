package scala.build.preprocessing.directives

import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.preprocessing.Scoped
import scala.build.options.BuildOptions
import scala.build.Positioned

import scala.util.Try
import scala.build.Position
import scala.compiletime.testing.ErrorKind
import caseapp.core.help.Help
import scala.build.preprocessing.directives.ScopedDirective
import scala.build.preprocessing.directives.ProcessedDirective
import com.virtuslab.using_directives.custom.model.EmptyValue
import com.virtuslab.using_directives.custom.model.Value
import scala.build.preprocessing.directives.DirectiveUtil
import com.virtuslab.using_directives.custom.model.StringValue
import scala.build.options.BuildRequirements
import scala.build.EitherCps.*
import scala.build.preprocessing.ScopePath
import scala.util.control.NonFatal

trait BuildOptionsUsingDirectiveHandler[V] extends SimpleDirectiveHandler[BuildOptions, V]{
  override def processScopedValues(scoped: Seq[Scoped[V]])(using Ctx) =
    scoped
      .flatMap(v => constrains.positions(v.value))
      .map(_.error(s"Scope is not supported in $name"))
      .sequenceToComposite

  protected def fromCommand(optName: String, help: Help[_]*): String = 
    val arg = help.flatMap(_.args).find(a => a.name.name == optName || a.extraNames.exists(_.name == optName))
      arg.fold("TODO no arg")(_.helpMessage.fold("TODO nod help")(_.message))
}

trait BuildRequirementsHandler[V] extends SimpleDirectiveHandler[BuildRequirements, V]

trait TargetDirectiveHandler[V] extends SimpleDirectiveHandler[BuildOptions, V]

case class UsingDirectiveError(msg: String, pos: Seq[Position], kind: UsingDirectiveError.Kind)
  extends BuildException(msg, pos)

object UsingDirectiveError {
  trait Kind:
    def error(msg: String)(positions: Seq[Position]) = 
      Left(UsingDirectiveError(msg, positions, this))

  object NoValueProvided extends Kind:
    def apply(types: Seq[DirectiveValue[_]], value: Option[Value[_]] = None)(using ctx: DirectiveContext) =
      val pos = value.fold(ctx.positionOnTop)(ctx.positionsFor(_))
      error(s"No value provided, expected at least one of ${types.mkString(", ")}")(pos)
  
  object MatchingMultipleValue extends Kind:
    def apply(types: Seq[DirectiveValue[_]]) = 
      error(s"Value matching is matching to multiple types: ${types.mkString(" | ")}")

  object NotMatching extends Kind:
    def apply(v: Value[_], expected: Seq[DirectiveValue[_]]) =
      // s"The value of type ${v.getClass().getSimpleName()} does not much any of the supported types: ${types}"
      error("TODO")

  object ExpectedSingle extends Kind:
    def apply(v: Seq[Value[_]], expected: Seq[DirectiveValue[_]])(using ctx: DirectiveContext) =
      // s"The value of type ${v.getClass().getSimpleName()} does not much any of the supported types: ${types}"
      error("TODO 123")(ctx.positionsFor(v:_*))
}

case class DirectiveContext(scopedDirective: ScopedDirective, logger: Logger) {
  def asRoot(pos: Positioned[_]): Either[BuildException, os.Path] =
    scopedDirective.cwd.root match {
      case Left(virtualRoot) =>
        pos.error(s"Can't reference paths from sources from $virtualRoot")
      case Right(root) =>
        Right(root / scopedDirective.cwd.path)
    }

  def positionsFor(vs: Value[_]*) = vs.flatMap { v =>
    val skipQuotes = v.isInstanceOf[StringValue]
    Seq(DirectiveUtil.position(v, scopedDirective.maybePath, skipQuotes))
  }

  def positionOnTop = 
    List(Position.File(scopedDirective.maybePath, (0,0), (0, 0)))
}

trait SimpleDirectiveHandler[T, V] extends DirectiveHandler[T] {
  type Ctx = DirectiveContext

  def constrains: DirectiveConstrains[V]

  def usagesCode: Seq[String]

  override def usage = usagesCode.mkString("\n\n")

  def process(v: V)(using Ctx): Either[BuildException, T]
  def processScopedValues(v: Seq[Scoped[V]])(using Ctx): Either[BuildException, Seq[Scoped[T]]] =
    v.map(_.mapEither(process)).sequenceToComposite

  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedDirective[T]] = {
    given Ctx(scopedDirective, logger)
    if (keys.contains(scopedDirective.directive.key)){
      val values = scopedDirective.directive.values match {
        case Seq(single) => Seq(single)
        case other => other.filterNot(_.isInstanceOf[EmptyValue])
      }
      // TODO do we really use scoped values?
      val (notScopedValues, scopedValues) = values.partition(_.getScope == null)
      val byScope = scopedValues.groupBy { v =>
        try Right(scopedDirective.cwd / os.RelPath(v.getScope))
        catch 
          case NonFatal(e) => 
            val positions = summon[DirectiveContext].positionsFor(v)
            Left(new BuildException("Invalid path used in scope", positions){}) // TODO dedicated error?
      }

      val global = 
        if notScopedValues.isEmpty && scopedValues.nonEmpty then Right(None)
        else constrains.extract(values).flatMap(process).map(Some(_))

      val scoped = byScope.toSeq.map {
        case (Left(e), _) => Left(e)
        case (Right(scope), values) => 
          constrains.extract(values).map(v => Scoped(scope, v))
      }.sequenceToComposite.flatMap(processScopedValues)
      
      either { ProcessedDirective(value(global), value(scoped)) }
    } else Right(ProcessedDirective(None, Nil))
  }
}

case class DirectiveHandlerGroup[T](name:String, handlers: Seq[DirectiveHandler[T]])

trait DirectiveHandler[T] {
  def name: String
  def description: String
  def descriptionMd: String = description
  def usage: String
  def usageMd: String       = s"`$usage`"
  def examples: Seq[String]

  def keys: Seq[String]

  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedDirective[T]]
}
