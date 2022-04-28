package scala.build.preprocessing.directives

import scala.build.Logger
import scala.build.errors.{
  BuildException,
  UsingDirectiveExpectationError,
  UsingDirectiveValueNumError,
  UsingDirectiveWrongValueTypeError
}
import scala.build.preprocessing.directives.UsingDirectiveValueKind.{
  BOOLEAN,
  EMPTY,
  NUMERIC,
  STRING,
  UsingDirectiveValueKind
}
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
import scala.build.preprocessing.directives.GroupedScopedValuesContainer
import scala.build.preprocessing.directives.UsingDirectiveValueNumberBounds
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

  /** It checks if the values assigned to the key of the scopedDirective have the expected type or
    * number.
    *
    * Note: the correct functioning of this method for some of the handlers might require overriding
    * the implementations for [[getSupportedTypes]], [[getValueNumberBounds]], and
    * [[unexpectedValueHint]] inside those handlers.
    *
    * @param scopedDirective
    *   the directive whose values are to be processed according to the key
    * @return
    *   Either a [[BuildException]] or the grouped using directive values inside the passed
    *   [[scopedDirective]]
    */
  final protected def checkIfValuesAreExpected(
    scopedDirective: ScopedDirective
  ): Either[BuildException, GroupedScopedValuesContainer] =
    receiveTheRightNumberOfValues(scopedDirective) flatMap (
      checkAndGroupValuesByType(_)
    )

  /** the default implementation only includes `UsingDirectiveValueKind.STRING` Override for
    * changing it.
    * @param key
    *   the using directive key for which the supported types are calculated.
    * @return
    *   a set of supported using directive value kinds of string, numeric, or boolean by this
    *   handler
    */
  protected def getSupportedTypes(key: String): Set[UsingDirectiveValueKind] =
    Set(scala.build.preprocessing.directives.UsingDirectiveValueKind.STRING)

  /** by default, it is `(lower = 1, upper = Int.MaxValue)`. Please, override for other cases.
    * @param key
    *   the using directive key for which the value number bounds are calculated.
    */
  protected def getValueNumberBounds(key: String): UsingDirectiveValueNumberBounds =
    UsingDirectiveValueNumberBounds()

  /** The default implementation just asks the user to put quotation marks around the using
    * directive values with string type.
    *
    * Therefore, it should get overrided if other information is to be shown instead in any
    * particular handler.
    *
    * This method gets internally called in the implementation of [[checkAndGroupValuesByType]] for
    * producing the error value.
    * @param key
    *   the using directive key for which the unexpected value(s) are passed.
    * @return
    *   the hint to be shown to the user if the value(s) passed to the corresponding using directive
    *   key are unexpected.
    */
  protected def unexpectedValueHint(key: String): String =
    s"Did you forget to put the quotation marks around the string values passed to the using directive key $key?"

  private def isEmptyValue(v: Value[_]) = v match {
    case _: EmptyValue => true
    case _             => false
  }

  /** checks if the passed in `scopedDirective` has the expected number of values according to the
    * implementation of [[getValueNumberBounds]] for the handler on which this is called
    *
    * @return
    *   Either a [[UsingDirectiveExpectationError]] or the same passed in scopedDirective
    */
  final protected def receiveTheRightNumberOfValues(
    scopedDirective: ScopedDirective
  ): Either[UsingDirectiveExpectationError, ScopedDirective] = {
    val nonEmptyValues: Seq[Value[_]] = scopedDirective.directive.values.filterNot(isEmptyValue(_))
    val length                        = nonEmptyValues.length
    val numberBounds                  = getValueNumberBounds(scopedDirective.directive.key)

    if (length < numberBounds.lower || length > numberBounds.upper)
      Left(new UsingDirectiveValueNumError(
        scopedDirective.maybePath,
        scopedDirective.directive.key,
        numberBounds,
        length
      ))
    else Right(scopedDirective.copy(directive =
      scopedDirective.directive.copy(values = nonEmptyValues)
    ))
  }

  /** It checks the values ascribed to the key inside the passed in `scopedDirective`, in order to
    * see if they are of the types listed to be expected by the implementation of
    * [[getSupportedTypes]] inside the specific handler.
    *
    * @return
    *   Either a [[UsingDirectiveExpectationError]] or the using directive values of the
    *   `scopedDirective` grouped in `string`, `boolean`, or `numeric` sequences.
    */
  final protected def checkAndGroupValuesByType(
    scopedDirective: ScopedDirective
  ): Either[UsingDirectiveExpectationError, GroupedScopedValuesContainer] = {
    val groupedPositionedValuesContainer = DirectiveUtil.getGroupedValues(scopedDirective)

    var groupedUnsupportedValues = GroupedScopedValuesContainer()

    val supportedTypes = getSupportedTypes(scopedDirective.directive.key)

    if (!supportedTypes.contains(BOOLEAN)) groupedUnsupportedValues =
      groupedUnsupportedValues.copy(scopedBooleanValues =
        groupedPositionedValuesContainer.scopedBooleanValues
      )
    if (!supportedTypes.contains(NUMERIC)) groupedUnsupportedValues =
      groupedUnsupportedValues.copy(scopedNumericValues =
        groupedPositionedValuesContainer.scopedNumericValues
      )
    if (!supportedTypes.contains(STRING))
      groupedUnsupportedValues = groupedUnsupportedValues.copy(scopedStringValues =
        groupedPositionedValuesContainer.scopedStringValues
      )
    if (!supportedTypes.contains(EMPTY))
      groupedUnsupportedValues = groupedUnsupportedValues.copy(maybeScopedEmptyValue =
        groupedPositionedValuesContainer.maybeScopedEmptyValue
      )

    if (groupedUnsupportedValues.isEmpty)
      Right(groupedPositionedValuesContainer)
    else Left(new UsingDirectiveWrongValueTypeError(
      maybePath = scopedDirective.maybePath,
      key = scopedDirective.directive.key,
      expectedTypes = supportedTypes,
      groupedUnsupportedValues,
      unexpectedValueHint(scopedDirective.directive.key)
    ))
  }

}
