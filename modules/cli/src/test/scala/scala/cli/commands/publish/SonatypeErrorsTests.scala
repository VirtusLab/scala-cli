package scala.cli.commands.publish

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.errors.{InvalidSonatypePublishCredentials, WrongSonatypeServerError}

class SonatypeErrorsTests extends munit.FunSuite {

  test("wrong server error on the Central Portal doesn't suggest switching to a legacy server") {
    val message = new WrongSonatypeServerError(isCentralPortal = true).getMessage
    expect(!message.contains("s01"))
    expect(!message.contains("Wrong Sonatype server"))
    expect(message.contains("https://central.sonatype.com"))
  }

  test("wrong server error on a legacy server suggests the Central Portal") {
    val message = new WrongSonatypeServerError(isCentralPortal = false).getMessage
    expect(message.contains("'central'"))
    expect(!message.contains("'central-s01'"))
  }

  test("invalid credentials on the Central Portal mention user tokens") {
    val message = new InvalidSonatypePublishCredentials(
      usernameIsAscii = true,
      passwordIsAscii = true,
      isCentralPortal = true
    ).getMessage
    expect(message.contains("Username or password to the publish repository are incorrect"))
    expect(message.contains("user token"))
  }

  test("invalid credentials on a legacy server keep the generic message") {
    val message = new InvalidSonatypePublishCredentials(
      usernameIsAscii = true,
      passwordIsAscii = true,
      isCentralPortal = false
    ).getMessage
    expect(message == "Username or password to the publish repository are incorrect")
  }

  test("invalid credentials with non-ASCII characters point at them") {
    val message = new InvalidSonatypePublishCredentials(
      usernameIsAscii = true,
      passwordIsAscii = false,
      isCentralPortal = true
    ).getMessage
    expect(message == "Your Sonatype password contains unsupported characters")
  }
}
