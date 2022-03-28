package scala.build.preprocessing.directives

case class UsingDirectiveValueNumberBounds(
  lower: Int = 1,
  upper: Int = Int.MaxValue
) {
  require(lower > -1)

  override def toString: String = s"at least at $lower and at most $upper num of values "
}

//object LowerBoundNumberOfExpectedValues extends Enumeration {
//  type LowerBoundNumberOfExpectedValues = Value
//  val AT_LEAST_ONE = Value(1)
//  val NO           = Value(0)
//}
//
//object UpperBoundNumberOfExpectedValues extends Enumeration {
//  type UpperBoundNumberOfExpectedValues = Value
//  val AT_MOST_ONE = Value(1)
//  val MULTIPLE    = Value(Int.MaxValue)
//}
