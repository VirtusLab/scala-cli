package scala.cli.commands.packager.debian

import os.PermSet

import scala.build.Logger
import scala.cli.commands.packager.NativePackager
import scala.sys.process._

case class DebianPackage(packageName: String, sourceAppPath: os.Path)
    extends NativePackager {

  private val debianBasePath = sourceAppPath / os.RelPath("../") / packageName
  private val usrDirectory = debianBasePath / "usr"
  private val packageInfo = buildDebianInfo()
  private val metaData = buildDebianMetaData(packageInfo)

  override def run(logger: Logger): Unit = {
    createConfFile
    createScriptFile
    copyExecutableFile

    logger.log(s"Starting build debian package to ${packageName} destination")
    println(s"Starting build debian package to ${packageName} destination")
    s"dpkg -b ./${packageName}".! match {
      case 0 => ()
      case errorCode =>
        System.err.println(
          s"Error building debian package, exit code: ${errorCode}"
        )
    }
  }

  private def buildDebianMetaData(info: DebianPackageInfo): DebianMetaData =
    DebianMetaData(
      debianInfo = info
    )

  private def buildDebianInfo(): DebianPackageInfo =
    DebianPackageInfo(
      packageName = "myapp",
      version = "1.0.0",
      maintainer = "test@gmail.com",
      description = "My test package",
      homepage = "https://github.com/lwronski/projectname"
    )

  private def copyExecutableFile: Unit = {
    val scalaDirectory = usrDirectory / "share" / "scala"
    os.makeDir.all(scalaDirectory)
    os.copy(sourceAppPath, scalaDirectory / packageName)
  }

  private def createConfFile = {
    val mainDebianDirectory = debianBasePath / "DEBIAN"
    os.makeDir.all(mainDebianDirectory)
    os.write(
      mainDebianDirectory / "control",
      metaData.generateMetaContent()
    )
  }

  private def createScriptFile: Unit = {
    val binDirectory = usrDirectory / "bin"
    os.makeDir.all(binDirectory)
    val launchScriptFile = binDirectory / packageName
    os.write(
      launchScriptFile,
      s"""#!/bin/bash
        |/usr/share/scala/${packageName}
        |""".stripMargin,
      PermSet.fromString("rwxrwxr-x")
    )
  }
}
