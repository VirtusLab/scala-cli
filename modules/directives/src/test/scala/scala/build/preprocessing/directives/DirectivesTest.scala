package scala.build.preprocessing.directives

import com.eed3si9n.expecty.Expecty.expect

class DirectivesTest extends munit.FunSuite {
  test("get directive handler by key") {
    val key     = "python"
    val handler = Directives.getDirectiveHandler(key).get
    expect(handler.keys.flatMap(_.nameAliases).contains(key))
    expect(handler.isExperimental)
    expect(handler.name == "Python")
    expect(handler.description.nonEmpty)
    expect(handler.descriptionMd.nonEmpty)
    expect(handler.usage.nonEmpty)
    expect(handler.usageMd.nonEmpty)
    expect(handler.examples.nonEmpty)
  }

}
