package scala.build.preprocessing.directives

import com.virtuslab.using_directives.custom.model.{EmptyValue, Value}

import java.util.Locale

import scala.build.Logger
import scala.build.Ops.*
import scala.build.directives.*
import scala.build.errors.{
  BuildException,
  CompositeBuildException,
  UnexpectedDirectiveError,
  UsingDirectiveExpectationError,
  UsingDirectiveValueNumError,
  UsingDirectiveWrongValueTypeError
}
import scala.build.preprocessing.Scoped
import scala.build.preprocessing.directives.UsingDirectiveValueKind.{
  BOOLEAN,
  EMPTY,
  NUMERIC,
  STRING,
  UsingDirectiveValueKind
}
import scala.cli.commands.SpecificationLevel
import scala.deriving.*
import scala.quoted.{_, given}

trait DirectiveHandler[+T] { self =>
  def name: String
  def description: String
  def descriptionMd: String = description
  def usage: String
  def usageMd: String       = s"`$usage`"
  def examples: Seq[String] = Nil

  /** Is this directive an advanved feature, that will not be accessible when running scala-cli as
    * `scala`
    */
  def scalaSpecificationLevel: SpecificationLevel
  protected def SpecificationLevel = scala.cli.commands.SpecificationLevel

  final def isRestricted: Boolean = scalaSpecificationLevel == SpecificationLevel.RESTRICTED

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

  def map[U](f: T => U): DirectiveHandler[U] =
    new DirectiveHandler[U] {
      def name                   = self.name
      def usage                  = self.usage
      override def usageMd       = self.usageMd
      def description            = self.description
      override def descriptionMd = self.descriptionMd
      override def examples      = self.examples

      def scalaSpecificationLevel = self.scalaSpecificationLevel

      def keys = self.keys

      def handleValues(scopedDirective: ScopedDirective, logger: Logger) =
        self.handleValues(scopedDirective, logger)
          .map(_.map(f))

      override protected def getSupportedTypes(key: String) =
        self.getSupportedTypes(key)
      override protected def getValueNumberBounds(key: String) =
        self.getValueNumberBounds(key)
      override protected def unexpectedValueHint(key: String) =
        self.unexpectedValueHint(key)
    }
  def mapE[U](f: T => Either[BuildException, U]): DirectiveHandler[U] =
    new DirectiveHandler[U] {
      def name                   = self.name
      def usage                  = self.usage
      override def usageMd       = self.usageMd
      def description            = self.description
      override def descriptionMd = self.descriptionMd
      override def examples      = self.examples

      def scalaSpecificationLevel = self.scalaSpecificationLevel

      def keys = self.keys

      def handleValues(scopedDirective: ScopedDirective, logger: Logger) =
        self.handleValues(scopedDirective, logger).flatMap(_.mapE(f))

      override protected def getSupportedTypes(key: String) =
        self.getSupportedTypes(key)
      override protected def getValueNumberBounds(key: String) =
        self.getValueNumberBounds(key)
      override protected def unexpectedValueHint(key: String) =
        self.unexpectedValueHint(key)
    }

}

object DirectiveHandler {

  // from https://github.com/alexarchambault/case-app/blob/7ac9ae7cc6765df48eab27c4e35c66b00e4469a7/core/shared/src/main/scala/caseapp/core/util/CaseUtil.scala#L5-L22
  def pascalCaseSplit(s: List[Char]): List[String] =
    if (s.isEmpty)
      Nil
    else if (!s.head.isUpper) {
      val (w, tail) = s.span(!_.isUpper)
      w.mkString :: pascalCaseSplit(tail)
    }
    else if (s.tail.headOption.forall(!_.isUpper)) {
      val (w, tail) = s.tail.span(!_.isUpper)
      (s.head :: w).mkString :: pascalCaseSplit(tail)
    }
    else {
      val (w, tail) = s.span(_.isUpper)
      if (tail.isEmpty)
        w.mkString :: pascalCaseSplit(tail)
      else
        w.init.mkString :: pascalCaseSplit(w.last :: tail)
    }

  def normalizeName(s: String): String = {
    val elems = s.split('-')
    (elems.head +: elems.tail.map(_.capitalize)).mkString
  }

  private def fields[U](using
    q: Quotes,
    t: Type[U]
  ): List[(q.reflect.Symbol, q.reflect.TypeRepr)] = {
    import quotes.reflect.*
    val tpe = TypeRepr.of[U]
    val sym = TypeRepr.of[U] match {
      case AppliedType(base, params) =>
        base.typeSymbol
      case _ =>
        TypeTree.of[U].symbol
    }

    // Many things inspired by https://github.com/plokhotnyuk/jsoniter-scala/blob/8f39e1d45fde2a04984498f036cad93286344c30/jsoniter-scala-macros/shared/src/main/scala-3/com/github/plokhotnyuk/jsoniter_scala/macros/JsonCodecMaker.scala#L564-L613
    // and around, here

    def typeArgs(tpe: TypeRepr): List[TypeRepr] = tpe match
      case AppliedType(_, typeArgs) => typeArgs.map(_.dealias)
      case _                        => Nil

    def resolveParentTypeArg(
      child: Symbol,
      fromNudeChildTarg: TypeRepr,
      parentTarg: TypeRepr,
      binding: Map[String, TypeRepr]
    ): Map[String, TypeRepr] =
      if (fromNudeChildTarg.typeSymbol.isTypeParam) { // todo: check for paramRef instead ?
        val paramName = fromNudeChildTarg.typeSymbol.name
        binding.get(paramName) match
          case None => binding.updated(paramName, parentTarg)
          case Some(oldBinding) =>
            if (oldBinding =:= parentTarg) binding
            else sys.error(
              s"Type parameter $paramName in class ${child.name} appeared in the constructor of " +
                s"${tpe.show} two times differently, with ${oldBinding.show} and ${parentTarg.show}"
            )
      }
      else if (fromNudeChildTarg <:< parentTarg)
        binding // TODO: assupe parentTag is covariant, get covariance from tycon type parameters.
      else
        (fromNudeChildTarg, parentTarg) match
          case (AppliedType(ctycon, ctargs), AppliedType(ptycon, ptargs)) =>
            ctargs.zip(ptargs).foldLeft(resolveParentTypeArg(child, ctycon, ptycon, binding)) {
              (b, e) =>
                resolveParentTypeArg(child, e._1, e._2, b)
            }
          case _ =>
            sys.error(s"Failed unification of type parameters of ${tpe.show} from child $child - " +
              s"${fromNudeChildTarg.show} and ${parentTarg.show}")

    def resolveParentTypeArgs(
      child: Symbol,
      nudeChildParentTags: List[TypeRepr],
      parentTags: List[TypeRepr],
      binding: Map[String, TypeRepr]
    ): Map[String, TypeRepr] =
      nudeChildParentTags.zip(parentTags).foldLeft(binding)((s, e) =>
        resolveParentTypeArg(child, e._1, e._2, s)
      )

    val nudeSubtype      = TypeIdent(sym).tpe
    val baseConst        = nudeSubtype.memberType(sym.primaryConstructor)
    val tpeArgsFromChild = typeArgs(tpe)
    val const = baseConst match {
      case MethodType(_, _, resTp) => resTp
      case PolyType(names, _, resPolyTp) =>
        val targs     = typeArgs(tpe)
        val tpBinding = resolveParentTypeArgs(sym, tpeArgsFromChild, targs, Map.empty)
        val ctArgs = names.map { name =>
          tpBinding.get(name).getOrElse(sys.error(
            s"Type parameter $name of $sym can't be deduced from " +
              s"type arguments of ${tpe.show}. Please provide a custom implicitly accessible codec for it."
          ))
        }
        val polyRes = resPolyTp match
          case MethodType(_, _, resTp) => resTp
          case other                   => other // hope we have no multiple typed param lists yet.
        if (ctArgs.isEmpty) polyRes
        else polyRes match
          case AppliedType(base, _) => base.appliedTo(ctArgs)
          case AnnotatedType(AppliedType(base, _), annot) =>
            AnnotatedType(base.appliedTo(ctArgs), annot)
          case _ => polyRes.appliedTo(ctArgs)
      case other =>
        sys.error(s"Primary constructor for ${tpe.show} is not MethodType or PolyType but $other")
    }
    sym.primaryConstructor
      .paramSymss
      .flatten
      .map(f => (f, f.tree))
      .collect {
        case (sym, v: ValDef) =>
          (sym, v.tpt.tpe)
      }
  }

  def shortName[T](using Quotes, Type[T]): String = {
    val fullName = Type.show[T]
    // attempt at getting a simple name out of fullName (this is likely broken)
    fullName.takeWhile(_ != '[').split('.').last
  }

  inline private def deriveParser[T]: DirectiveHandler[T] =
    ${ deriveParserImpl[T] }
  private def deriveParserImpl[T](using q: Quotes, t: Type[T]): Expr[DirectiveHandler[T]] = {
    import quotes.reflect.*
    val tSym    = TypeTree.of[T].symbol
    val origin  = shortName[T]
    val fields0 = fields[T]

    val defaultMap: Map[String, Expr[Any]] = {
      val comp =
        if (tSym.isClassDef && !tSym.companionClass.isNoSymbol) tSym.companionClass
        else tSym
      val bodyOpt = Some(comp)
        .filter(!_.isNoSymbol)
        .map(_.tree)
        .collect {
          case cd: ClassDef => cd.body
        }
      bodyOpt match {
        case Some(body) =>
          val names = fields0
            .map(_._1)
            .filter(_.flags.is(Flags.HasDefault))
            .map(_.name)
          val values = body.collect {
            case d @ DefDef(name, _, _, _) if name.startsWith("$lessinit$greater$default") =>
              Ref(d.symbol).asExpr
          }
          names.zip(values).toMap
        case None =>
          Map.empty
      }
    }

    val nameValue = tSym.annotations
      .find(_.tpe =:= TypeRepr.of[DirectiveGroupName])
      .collect {
        case Apply(_, List(arg)) =>
          arg.asExprOf[String]
      }
      .getOrElse {
        Expr(shortName[T].stripSuffix("Directives"))
      }

    val (usageValue, usageMdValue) = tSym.annotations
      .find(_.tpe =:= TypeRepr.of[DirectiveUsage])
      .collect {
        case Apply(_, List(arg)) =>
          (arg.asExprOf[String], Expr(""))
        case Apply(_, List(arg, argMd)) =>
          (arg.asExprOf[String], argMd.asExprOf[String])
      }
      .getOrElse {
        sys.error(s"Missing DirectiveUsage directive on ${Type.show[T]}")
      }

    val (descriptionValue, descriptionMdValue) = tSym.annotations
      .find(_.tpe =:= TypeRepr.of[DirectiveDescription])
      .collect {
        case Apply(_, List(arg)) =>
          (arg.asExprOf[String], Expr(""))
        case Apply(_, List(arg, argMd)) =>
          (arg.asExprOf[String], argMd.asExprOf[String])
      }
      .getOrElse {
        sys.error(s"Missing DirectiveDescription directive on ${Type.show[T]}")
      }

    val prefixValueOpt = tSym.annotations
      .find(_.tpe =:= TypeRepr.of[DirectivePrefix])
      .collect {
        case Apply(_, List(arg)) =>
          arg.asExprOf[String]
      }
    def withPrefix(name: Expr[String]): Expr[String] =
      prefixValueOpt match {
        case None              => name
        case Some(prefixValue) => '{ $prefixValue + $name }
      }

    val examplesValue = tSym.annotations
      .filter(_.tpe =:= TypeRepr.of[DirectiveExamples])
      .collect {
        case Apply(_, List(arg)) =>
          arg.asExprOf[String]
      }
      .reverse // not sure in what extent we can rely on the ordering here…

    val levelValue = tSym.annotations
      .find(_.tpe =:= TypeRepr.of[DirectiveLevel])
      .collect {
        case Apply(_, List(arg)) =>
          arg.asExprOf[SpecificationLevel]
      }
      .getOrElse {
        sys.error(s"Missing DirectiveLevel directive on ${Type.show[T]}")
      }

    def namesFromAnnotations(sym: Symbol) = sym.annotations
      .filter(_.tpe =:= TypeRepr.of[DirectiveName])
      .collect {
        case Apply(_, List(arg)) =>
          withPrefix(arg.asExprOf[String])
      }

    val keysValue = Expr.ofList {
      fields0.flatMap {
        case (sym, _) =>
          withPrefix(Expr(sym.name)) +: namesFromAnnotations(sym)
      }
    }

    val elseCase: (
      Expr[ScopedDirective],
      Expr[Logger]
    ) => Expr[Either[BuildException, ProcessedDirective[T]]] =
      (scopedDirective, _) =>
        '{
          Left(new UnexpectedDirectiveError($scopedDirective.directive.key))
        }

    val handleValuesImpl = fields0.zipWithIndex.foldRight(elseCase) {
      case (((sym, tRepr), idx), elseCase0) =>
        val namesFromAnnotations0 = namesFromAnnotations(sym)

        def typeArgs(tpe: TypeRepr): List[TypeRepr] = tpe match
          case AppliedType(_, typeArgs) => typeArgs.map(_.dealias)
          case _                        => Nil

        // from https://github.com/plokhotnyuk/jsoniter-scala/blob/1704a9cbb22b75a59f21ddf2a11427ba24df3212/jsoniter-scala-macros/shared/src/main/scala-3/com/github/plokhotnyuk/jsoniter_scala/macros/JsonCodecMaker.scala#L849-L854
        def genNew(argss: List[List[Term]]): Term =
          val constructorNoTypes = Select(New(Inferred(TypeRepr.of[T])), tSym.primaryConstructor)
          val constructor = typeArgs(TypeRepr.of[T]) match
            case Nil      => constructorNoTypes
            case typeArgs => TypeApply(constructorNoTypes, typeArgs.map(Inferred(_)))
          argss.tail.foldLeft(Apply(constructor, argss.head))((acc, args) => Apply(acc, args))

        val newArgs = fields0.map {
          case (sym, _) =>
            defaultMap.getOrElse(sym.name, sys.error(s"Field ${sym.name} has no default value"))
        }

        tRepr.asType match {
          case '[t] =>
            val parser = Expr.summon[DirectiveValueParser[t]].getOrElse {
              sys.error(s"Cannot get implicit DirectiveValueParser[${Type.show[t]}]")
            }

            val name = withPrefix(Expr(sym.name))

            val cond: Expr[String] => Expr[Boolean] =
              if (namesFromAnnotations0.isEmpty)
                keyName => '{ DirectiveHandler.normalizeName($keyName) == $name }
              else {
                val names = Expr.ofList(name +: namesFromAnnotations0)
                keyName => '{ $names.contains(DirectiveHandler.normalizeName($keyName)) }
              }

            (scopedDirective, logger) =>
              '{
                if (${ cond('{ $scopedDirective.directive.key }) }) {
                  val valuesByScope = $scopedDirective.directive.values.groupBy(_.getScope)
                    .toVector
                    .map {
                      case (scopeOrNull, values) =>
                        (Option(scopeOrNull), values)
                    }
                    .sortBy(_._1.getOrElse(""))
                  valuesByScope
                    .map {
                      case (scopeOpt, values) =>
                        $parser.parse(
                          $scopedDirective.directive.values,
                          $scopedDirective.cwd,
                          $scopedDirective.maybePath
                        ).map { r =>
                          scopeOpt -> ${
                            genNew(List(newArgs.updated(idx, '{ r }).map(_.asTerm)))
                              .asExprOf[T]
                          }
                        }
                    }
                    .sequence
                    .left.map(CompositeBuildException(_))
                    .map { v =>
                      val mainOpt = v.collectFirst {
                        case (None, t) => t
                      }
                      val scoped = v.collect {
                        case (Some(scopeStr), t) =>
                          // FIXME os.RelPath(…) might fail
                          Scoped(
                            $scopedDirective.cwd / os.RelPath(scopeStr),
                            t
                          )
                      }
                      ProcessedDirective(mainOpt, scoped)
                    }
                }
                else
                  ${ elseCase0(scopedDirective, logger) }
              }
        }
    }

    '{
      new DirectiveHandler[T] {
        def name  = $nameValue
        def usage = $usageValue
        override def usageMd =
          Some($usageMdValue).filter(_.nonEmpty).getOrElse(usage)
        def description = $descriptionValue
        override def descriptionMd =
          Some($descriptionMdValue).filter(_.nonEmpty).getOrElse(description)
        override def examples = ${ Expr.ofList(examplesValue) }

        def scalaSpecificationLevel = $levelValue

        lazy val keys = $keysValue
          .flatMap(key =>
            List(
              key,
              DirectiveHandler.pascalCaseSplit(key.toCharArray.toList)
                .map(_.toLowerCase(Locale.ROOT))
                .mkString("-")
            )
          )
          .distinct

        def handleValues(scopedDirective: ScopedDirective, logger: Logger) =
          ${ handleValuesImpl('{ scopedDirective }, '{ logger }) }
      }
    }
  }

  inline given derive[T]: DirectiveHandler[T] =
    DirectiveHandler.deriveParser[T]
}
