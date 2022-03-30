package scala.build.options

case class Inner(
  foo: Boolean = false,
  bar: Seq[String] = Nil,
  baz: Set[Double] = Set()
)

object Inner {
  import ConfigMonoid.*
  implicit def monoid: ConfigMonoid[Inner] = ConfigMonoid.derive
}

case class Outer(
  name: Option[String] = None,
  inner: Inner = Inner()
)

object Outer {
  implicit def monoid: ConfigMonoid[Outer] = ConfigMonoid.derive
}

class ConfigMonoidTest extends munit.FunSuite {
  test("Basic Config Monoid") {
    val inner1 = Inner(foo = true)

    assertEquals(false, Inner.monoid.zero.foo)
    assertEquals(true, Inner.monoid.orElse(inner1, Inner()).foo)
    assertEquals(true, Inner.monoid.orElse(Inner(), inner1).foo)

    val outer = Outer(inner = inner1)

    assertEquals(false, Outer.monoid.zero.inner.foo)
    assertEquals(true, Outer.monoid.orElse(outer, Outer()).inner.foo)
    assertEquals(true, Outer.monoid.orElse(Outer(), outer).inner.foo)
  }

  test("Merging sets") {
    val inner1 = Inner(bar = Seq("v1"))
    val inner2 = Inner(bar = Seq("v2"))

    assertEquals(Seq("v1"), Inner.monoid.orElse(inner1, Inner()).bar)
    assertEquals(Seq("v1", "v2"), Inner.monoid.orElse(inner1, inner2).bar)
    assertEquals(Seq("v2", "v1"), Inner.monoid.orElse(inner2, inner1).bar)

    val outer1 = Outer(Some("o1"), inner1)
    val outer2 = Outer(Some("o2"), inner2)

    assertEquals(Seq("v1"), Outer.monoid.orElse(outer1, Outer()).inner.bar)
    assertEquals(Seq("v1", "v2"), Outer.monoid.orElse(outer1, outer2).inner.bar)
    assertEquals(Seq("v2", "v1"), Outer.monoid.orElse(outer2, outer1).inner.bar)

    assertEquals(Outer.monoid.orElse(outer1, outer2).name, Some("o1"))
    assertEquals(Outer.monoid.orElse(outer2, outer1).name, Some("o2"))

  }
}
