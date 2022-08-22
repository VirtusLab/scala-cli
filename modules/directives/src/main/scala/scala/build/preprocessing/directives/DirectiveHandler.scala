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

trait DirectiveHandler[T] {
  def name: String
  def description: String
  def descriptionMd: String = description
  def usage: String
  def usageMd: String       = s"`$usage`"
  def examples: Seq[String] = Nil

  /** Is this directive an advanved feature, that will not be accessible when running scala-cli as
    * `scala`
    */
  def isRestricted: Boolean

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
