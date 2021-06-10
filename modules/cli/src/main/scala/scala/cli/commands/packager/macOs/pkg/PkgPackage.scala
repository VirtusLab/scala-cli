package scala.cli.commands.packager.macOs.pkg

import os.PermSet

import scala.build.Logger
import scala.cli.commands.packager.macOs.MacOsNativePackager

case class PkgPackage (sourceAppPath: os.Path, packageName: String)
  extends MacOsNativePackager {

  private val scriptsPath = basePath / "scripts"

  override def run(logger: Logger): Unit = {

    createAppDirectory()
    createInfoPlist()
    createScriptFile()

    os.proc("pkgbuild", "--install-location", "/Applications", "--component", s"$packageName.app",  s"$packageName.pkg", "--scripts", scriptsPath)
      .call(cwd = basePath)

    postInstallClean()
  }

  private def postInstallClean() = {
    os.remove.all(macOsAppPath)
    os.remove.all(scriptsPath)
  }

    private def createScriptFile()= {
    val content = s"""#!/bin/bash
                    |rm -f /usr/local/bin/$packageName
                    |ln -s /Applications/$packageName.app/Contents/MacOS/$packageName /usr/local/bin/$packageName""".stripMargin
    os.makeDir.all(scriptsPath)
    os.write(scriptsPath / "postinstall", content, PermSet.fromString("rwxrwxr-x"))
  }

}


