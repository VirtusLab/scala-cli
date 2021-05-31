/*
 * Scala (https://www.scala-lang.org)
 *
 * Copyright EPFL and Lightbend, Inc.
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

// Originally adapted from https://github.com/scala/scala/blob/20ac944346a93ba747811e80f8f67a09247cb987/src/compiler/scala/tools/tasty/TastyName.scala

package scala.cli.tastylib

import scala.reflect.NameTransformer

object TastyName {

  final case class SimpleName(raw: String)                                                    extends TastyName
  final case class ObjectName(base: TastyName)                                                extends TastyName
  final case class QualifiedName(qual: TastyName, sep: SimpleName, selector: SimpleName)      extends TastyName
  final case class UniqueName(qual: TastyName, sep: SimpleName, num: Int)                     extends TastyName
  final case class DefaultName(qual: TastyName, num: Int)                                     extends TastyName
  final case class PrefixName(prefix: SimpleName, qual: TastyName)                            extends TastyName
  final case class SuffixName(qual: TastyName, suffix: SimpleName)                            extends TastyName
  final case class TypeName private (base: TastyName)                                         extends TastyName
  final case class SignedName(
    qual: TastyName,
    sig: Signature.MethodSignature[ErasedTypeRef],
    target: TastyName) extends TastyName

  object TypeName {
    private[TastyName] def apply(base: TastyName): TypeName = base match {
      case name: TypeName => name
      case name           => new TypeName(name)
    }
  }

  // Separators
  final val PathSep: SimpleName = SimpleName(".")
  final val ExpandedSep: SimpleName = SimpleName("$$")
  final val ExpandPrefixSep: SimpleName = SimpleName("$")
  final val InlinePrefix: SimpleName = SimpleName("inline$")
  final val SuperPrefix: SimpleName = SimpleName("super$")
  final val BodyRetainerSuffix: SimpleName = SimpleName("$retainedBody")

  // TermNames
  final val Empty: SimpleName = SimpleName("")
  final val Constructor: SimpleName = SimpleName("<init>")

  final val DefaultGetterStr     = "$default$"
  final val DefaultGetterInitStr = NameTransformer.encode("<init>") + DefaultGetterStr

  trait NameEncoder[U] {
    final def encode[O](name: TastyName)(init: => U, finish: U => O): O = finish(traverse(init, name))
    def traverse(u: U, name: TastyName): U
  }

  trait StringBuilderEncoder extends NameEncoder[StringBuilder] {
    final def encode(name: TastyName): String = name match {
      case SimpleName(raw) => raw
      case _               => super.encode(name)(new StringBuilder(25), _.toString)
    }
  }

  object SourceEncoder extends StringBuilderEncoder {
    def traverse(sb: StringBuilder, name: TastyName): StringBuilder = name match {
      case name: SimpleName    => sb append (name.raw)
      case name: ObjectName    => traverse(sb, name.base)
      case name: TypeName      => traverse(sb, name.base)
      case name: SignedName    => traverse(sb, name.qual)
      case name: UniqueName    => traverse(sb, name.qual) append (name.sep.raw) append (name.num)
      case name: QualifiedName => traverse(traverse(sb, name.qual) append (name.sep.raw), name.selector)
      case name: PrefixName    => traverse(sb append (name.prefix.raw), name.qual)
      case name: SuffixName    => traverse(sb, name.qual) append (name.suffix.raw)

      case name: DefaultName if name.qual == Constructor  =>
        sb append DefaultGetterInitStr append (name.num + 1)

      case name: DefaultName => traverse(sb, name.qual) append DefaultGetterStr append (name.num + 1)
    }
  }

  object DebugEncoder extends StringBuilderEncoder {
    import Signature.merge

    def traverse(sb: StringBuilder, name: TastyName): StringBuilder = name match {
      case SimpleName(raw)               => sb append raw
      case DefaultName(qual, num)        => traverse(sb, qual) append "[Default " append (num + 1) append ']'
      case PrefixName(prefix, qual)      => traverse(sb, qual) append "[Prefix " append (prefix.raw) append ']'
      case SuffixName(qual, suffix)      => traverse(sb, qual) append "[Suffix " append (suffix.raw) append ']'
      case ObjectName(name)              => traverse(sb, name) append "[ModuleClass]"
      case TypeName(name)                => traverse(sb, name) append "[Type]"
      case SignedName(name, sig, target) => merge(traverse(sb, name) append "[Signed ", sig.map(_.signature)) append " @" append target.source append ']'
      case QualifiedName(qual, sep, name) =>
        traverse(sb, qual) append "[Qualified " append (sep.raw) append ' ' append (name.raw) append ']'
      case UniqueName(qual, sep, num) =>
        traverse(sb, qual) append "[Unique " append (sep.raw) append ' ' append num append ']'
    }
  }

  object ScalaNameEncoder extends NameEncoder[StringBuilder] {

    final def encode(name: TastyName): String = name match {
      case SimpleName(raw) => NameTransformer.encode(raw)
      case _               => super.encode(name)(new StringBuilder(25), _.toString)
    }

    def traverse(sb: StringBuilder, name: TastyName): StringBuilder = name match {
      case name: SimpleName    => sb.append(NameTransformer.encode(name.raw))
      case name: ObjectName    => traverse(sb, name.base)
      case name: TypeName      => traverse(sb, name.base)
      case name: SignedName    => traverse(sb, name.qual)
      case name: UniqueName    => traverse(sb, name.qual) append (name.sep.raw) append (name.num)
      case name: QualifiedName => traverse(traverse(sb, name.qual) append (name.sep.raw), name.selector)
      case name: PrefixName    => traverse(sb append (name.prefix.raw), name.qual)
      case name: SuffixName    => traverse(sb, name.qual) append (name.suffix.raw)
      case name: DefaultName if name.qual == Constructor => sb append DefaultGetterInitStr append (name.num + 1)
      case name: DefaultName => traverse(sb, name.qual) append DefaultGetterStr append (name.num + 1)
    }
  }

  def deepEncode(name: TastyName): TastyName = name match {
    case SimpleName(raw) => SimpleName(NameTransformer.encode(raw))
    case QualifiedName(qual, sep, selector) => QualifiedName(deepEncode(qual), sep, deepEncode(selector).asSimpleName)
    case ObjectName(base) => ObjectName(deepEncode(base))
    case UniqueName(qual, sep, num) => UniqueName(deepEncode(qual), sep, num)
    case DefaultName(qual, num) => DefaultName(deepEncode(qual), num)
    case PrefixName(prefix, qual) => PrefixName(prefix, deepEncode(qual))
    case SuffixName(qual, suffix) => SuffixName(deepEncode(qual), suffix)
    case TypeName(base) => TypeName(deepEncode(base))
    case SignedName(qual, sig, target) => SignedName(deepEncode(qual), sig.map(_.encode), target)
  }
}

sealed abstract class TastyName extends Product with Serializable { self =>
  import TastyName._

  final override def toString: String = source

  final def isObjectName: Boolean = self.isInstanceOf[ObjectName]

  final def asSimpleName: SimpleName = self match {
    case self: SimpleName => self
    case _                => throw new AssertionError(s"not simplename: ${self.debug}")
  }

  final def source: String = SourceEncoder.encode(self)

  final def debug: String = DebugEncoder.encode(self)

  final def toTermName: TastyName = self match {
    case TypeName(name) => name
    case name           => name
  }

  final def toTypeName: TypeName = TypeName(self)
}
