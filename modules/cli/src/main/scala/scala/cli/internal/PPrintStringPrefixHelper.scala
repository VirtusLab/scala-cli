package scala.cli.internal

// Remove once we can use https://github.com/com-lihaoyi/PPrint/pull/80

final class PPrintStringPrefixHelper {
  def apply(i: Iterable[Object]): String =
    i.collectionClassName
}
