package scala.build.preprocessing.directives


import com.virtuslab.using_directives.custom.model.*
import scala.build.errors.BuildException
import scala.build.Positioned

sealed trait DirectiveConstrains[R] {
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

  def positions(v: R): Seq[Positioned[_]]
}

case class Single[V](val types: DirectiveValue[V]*)
    extends DirectiveConstrains[Positioned[V]] {
  def extract(values: Seq[Value[_]])(using ctx: DirectiveContext): Either[BuildException, Positioned[V]] = values match {
    case Seq(v: EmptyValue) => UsingDirectiveError.NoValueProvided(types, Some(v))
    case Seq(s) => fromTypes(s, types)
    case Nil     => UsingDirectiveError.NoValueProvided(types)
    case multiple => UsingDirectiveError.ExpectedSingle(multiple, types)
  }

  def positions(v: Positioned[V]): Seq[Positioned[_]] = Seq(v)
}

case class AtLeastOne[V](types: DirectiveValue[V]*)
    extends DirectiveConstrains[::[Positioned[V]]] {
  def extract(values: Seq[Value[_]])(using DirectiveContext): Either[BuildException, ::[Positioned[V]]] =
    values match 
      case Nil =>  UsingDirectiveError.NoValueProvided(types)
      case Seq(e: EmptyValue) => UsingDirectiveError.NoValueProvided(types, Some(e))
      case _ =>
        Seq(1).map(_ + 1)
        values.map(fromTypes(_, types)).sequenceToComposite.map(l => ::(l.head, l.tail.toList))

  def positions(v: ::[Positioned[V]]): Seq[Positioned[_]] = v
}
