package scala.cli.commands.publish

import coursier.core.{ModuleName, Organization, Type}
import coursier.publish.Pom

import java.time.LocalDateTime

class IvyTests extends munit.FunSuite {

  test("ivy includes Apache Ivy license and Maven POM scm and developers") {
    val organization = Organization("org.example")
    val moduleName   = ModuleName("demo")
    val version      = "1.0"
    val description  = "A demo"
    val homepage     = "https://example.org"

    val pomProjectName = "Demo library"
    val packaging      = Type("jar")

    val licenseName = "Apache-2.0"
    val licenseUrl  = "https://spdx.org/licenses/Apache-2.0.html"

    val scmUrl           = "https://github.com/foo/bar.git"
    val scmConnection    = "scm:git:github.com/foo/bar.git"
    val scmDevConnection = "scm:git:git@github.com:foo/bar.git"

    val devId     = "jdu"
    val devName   = "Jane"
    val devUrl    = "https://jane.example"
    val devMail   = "jane@example.org"
    val fixedTime = LocalDateTime.of(2024, 1, 2, 3, 4, 5)

    val xml = Ivy.create(
      organization = organization,
      moduleName = moduleName,
      version = version,
      description = Some(description),
      url = Some(homepage),
      pomProjectName = Some(pomProjectName),
      packaging = Some(packaging),
      license = Some(Pom.License(licenseName, licenseUrl)),
      scm = Some(Pom.Scm(scmUrl, scmConnection, scmDevConnection)),
      developers = Seq(
        Pom.Developer(devId, devName, devUrl, Some(devMail))
      ),
      dependencies = Nil,
      time = fixedTime
    )
    assert(xml.contains(s"""<license name="$licenseName""""))
    assert(xml.contains(s"""url="$licenseUrl""""))
    assert(xml.contains(s"<m:name>$pomProjectName</m:name>"))
    assert(xml.contains(s"<m:packaging>${packaging.value}</m:packaging>"))
    assert(xml.contains(s"<m:url>$scmUrl</m:url>"))
    assert(xml.contains(s"<m:connection>$scmConnection</m:connection>"))
    assert(xml.contains(s"<m:developerConnection>$scmDevConnection</m:developerConnection>"))
    assert(xml.contains(s"<m:id>$devId</m:id>"))
    assert(xml.contains(s"<m:name>$devName</m:name>"))
    assert(xml.contains(s"<m:url>$devUrl</m:url>"))
    assert(xml.contains(s"<m:email>$devMail</m:email>"))
    assert(xml.contains("xmlns:m="))
  }

  test("ivy omits Maven namespace when there is no scm or developer XML") {
    val organization = Organization("org.example")
    val moduleName   = ModuleName("demo")
    val version      = "1.0"
    val licenseName  = "MIT"
    val licenseUrl   = "https://opensource.org/licenses/MIT"
    val fixedTime    = LocalDateTime.of(2024, 1, 2, 3, 4, 5)

    val xml = Ivy.create(
      organization = organization,
      moduleName = moduleName,
      version = version,
      license = Some(Pom.License(licenseName, licenseUrl)),
      pomProjectName = None,
      packaging = None,
      scm = Some(Pom.Scm("", "", "")),
      developers = Nil,
      time = fixedTime
    )
    assert(!xml.contains("xmlns:m="))
    assert(xml.contains(s"""<license name="$licenseName""""))
    assert(xml.contains(s"""url="$licenseUrl""""))
  }

}
