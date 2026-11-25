package scala.cli.commands.publish.checks

import com.eed3si9n.expecty.Expecty.expect

import scala.build.Positioned
import scala.build.options.publish.Developer

class DeveloperCheckTests extends munit.FunSuite {

  test("developer directive value follows the id|name|URL|email format") {
    val value = DeveloperCheck.directiveValue(
      name = "Alex Test",
      email = "alex@alex.me",
      url = "https://github.com/alextest"
    )
    expect(value == "alextest|Alex Test|https://github.com/alextest|alex@alex.me")
    expect(
      Developer.parse(Positioned.none(value)) ==
        Right(Developer(
          "alextest",
          "Alex Test",
          "https://github.com/alextest",
          Some("alex@alex.me")
        ))
    )
  }

  test("developer ID falls back to the name for non-GitHub URLs") {
    expect(DeveloperCheck.developerId("Alex Test", "https://alex.me") == "Alex Test")
    expect(DeveloperCheck.developerId("Alex Test", "https://github.com/alextest/") == "alextest")
    expect(DeveloperCheck.developerId("Alex Test", "https://github.com/org/repo") == "Alex Test")
  }
}
