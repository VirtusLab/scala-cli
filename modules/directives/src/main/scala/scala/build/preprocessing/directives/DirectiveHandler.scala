package scala.build.preprocessing.directives
import com.virtuslab.using_directives.custom.model.{EmptyValue, Value}

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
import com.virtuslab.using_directives.custom.model.StringValue
import com.virtuslab.using_directives.custom.model.BooleanValue
import com.virtuslab.using_directives.custom.model.NumericValue
import scala.util.Try
import scala.build.Position
import scala.compiletime.testing.ErrorKind

sealed trait DirectiveValue[Out] {
  def extract(v: Value[_]): Option[Out]
}

object ValueType {
  case object String extends DirectiveValue[String] {
    def extract(v: Value[_]): Option[String] = v match {
      case s: StringValue => Some(s.get())
      case _ => None
    }
  }
  case object Boolean extends DirectiveValue[Boolean] {
    def extract(v: Value[_]): Option[Boolean] = v match {
      case s: BooleanValue => Some(s.get())
      case _: EmptyValue => Some(true) // TODO it that ok?
      case _ => None
    }
  
  }
  case object Number extends DirectiveValue[String] {
    def extract(v: Value[_]): Option[String] = v match {
      case n: NumericValue => Some(n.get())
      case _ => None
    }
  }  
}

sealed trait DirectiveConstrains[R, U] {
  def extract(values: Seq[Value[_]])(using DirectiveContext): Either[BuildException, R]

  final protected def fromTypes[V](v: Value[_], types: Seq[DirectiveValue[V]])(using DirectiveContext): Either[BuildException, Positioned[V]] = {
    val positions = summon[DirectiveContext].positionsFor(v)
    types.flatMap(_.extract(v)) match {
      case Seq(single) =>
        Right(Positioned(positions, single))
      case Seq() => 
        UsingDirectiveError.NotMatching(v, types)(positions)
        
      case _ =>
        val matchingTypes = types.filter(_.extract(v).nonEmpty)
        UsingDirectiveError.MatchingMultipleValue(matchingTypes)(positions)
    }
  }

  def types: Seq[DirectiveValue[_]]
}

case class Single[V](val types: DirectiveValue[V]*)
    extends DirectiveConstrains[Positioned[V], Any] {
  def extract(values: Seq[Value[_]])(using ctx: DirectiveContext): Either[BuildException, Positioned[V]] = values match {
    case Seq(v: EmptyValue) => UsingDirectiveError.NoValueProvided(types, Some(v))
    case Seq(s) => fromTypes(s, types)
    case Nil     => UsingDirectiveError.NoValueProvided(types)
    case multiple => UsingDirectiveError.ExpectedSingle(multiple, types)
  }
}

case class AtLeastOne[V](types: DirectiveValue[V]*)
    extends DirectiveConstrains[::[Positioned[V]], Seq[String]] {
  def extract(values: Seq[Value[_]])(using DirectiveContext): Either[BuildException, ::[Positioned[V]]] =
    values match 
      case Nil =>  UsingDirectiveError.NoValueProvided(types)
      case Seq(e: EmptyValue) => UsingDirectiveError.NoValueProvided(types, Some(e))
      case _ =>
        Seq(1).map(_ + 1)
        values.map(fromTypes(_, types)).sequenceToComposite.map(l => ::(l.head, l.tail.toList))
}

abstract class SimpleUsingDirectiveHandler[V, U](
  val name: String,
  val description: String,
  val keys: Seq[String],
  val constrains: DirectiveConstrains[V, U]
) extends SimpleDirectiveHandler[BuildOptions, V, U] {

  type Ctx = DirectiveContext

  def process(v: V)(using ctx: Ctx): Either[BuildException, BuildOptions]

  def processGlobalValue(v: V)(using DirectiveContext): Either[BuildException, Option[BuildOptions]] 
    = process(v).map(Some(_))
  def processScopedValue(v: V)(using DirectiveContext): Either[BuildException, Seq[Scoped[BuildOptions]]] 
    = Right(Nil)
}

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

trait SimpleDirectiveHandler[T, V, U] extends DirectiveHandler[T] {

  def constrains: DirectiveConstrains[V, U]

  def usagesCode: Seq[String]

  override def usage = usagesCode.mkString("\n\n")

  def processGlobalValue(v: V)(using DirectiveContext): Either[BuildException, Option[T]]
  def processScopedValue(v: V)(using DirectiveContext): Either[BuildException, Seq[Scoped[T]]]

  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedDirective[T]] = {
    given DirectiveContext(scopedDirective, logger)
    if (keys.contains(scopedDirective.directive.key)){
      val values = scopedDirective.directive.values match {
        case Seq(single) => Seq(single)
        case other => other.filterNot(_.isInstanceOf[EmptyValue])
      }
      for {
        wrapped <- constrains.extract(values)
        global  <- processGlobalValue(wrapped)
        scoped  <- processScopedValue(wrapped)
      } yield ProcessedDirective(global, scoped)
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
    Set(UsingDirectiveValueKind.STRING)

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
