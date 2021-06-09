package scala.cli.commands.packager.macOs

import scala.cli.commands.packager.NativePackager

trait MacOsNativePackager extends NativePackager {

  val macOsAppPath  = basePath / s"$packageName.app"
  val contentPath = macOsAppPath / "Contents"
  val macOsPath = contentPath / "MacOS"
  val infoPlist = MacOsInfoPlist(packageName, s"com.example.$packageName")

  protected def createAppDirectory() = {
    os.makeDir.all(macOsPath)
    os.copy(sourceAppPath, macOsPath / packageName)
  }

  protected def createInfoPlist() = {
    os.write(contentPath / "Info.plist", infoPlist.generateContent)
  }
}
