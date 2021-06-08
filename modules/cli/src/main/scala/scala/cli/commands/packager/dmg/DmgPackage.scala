package scala.cli.commands.packager.dmg

import scala.build.Logger
import scala.cli.commands.packager.NativePackager
import scala.sys.process._

case class DmgPackage(packageName: String, sourceAppPath: os.Path)
  extends NativePackager {

  private val dmgAppPath = sourceAppPath / os.RelPath("../") / s"$packageName.app"
  private val contentPath = dmgAppPath / "Contents"
  private val macOsPath = contentPath / "MacOS"
  private val mountpointPath = sourceAppPath / os.RelPath("../") / "mountpoint"

  override def run(logger: Logger): Unit = {
    s"hdiutil create -megabytes 100  -fs HFS+ -volname $packageName  $packageName".! match {
      case 0 => ()
      case errorCode =>
        System.err.println(
          s"Error creating disk image, exit code: ${errorCode}"
        )
    }

    createAppDirectory()
    createInfoPlist()

    s"hdiutil attach ${packageName}.dmg -readwrite -mountpoint  mountpoint/".! match {
      case 0 => ()
      case errorCode =>
        System.err.println(
          s"Error attaching mountpoint, exit code: ${errorCode}"
        )
    }

    copyAppDirectory()

    s"hdiutil detach mountpoint/".! match {
      case 0 => ()
      case errorCode =>
        System.err.println(
          s"Error detaching mountpoint, exit code: ${errorCode}"
        )
    }

//    os.remove.all(dmgAppPath)

  }

  private def copyAppDirectory() = {
    os.copy(dmgAppPath, mountpointPath / s"$packageName.app")
    os.symlink(mountpointPath / "Applications", os.root / "Applications" )
  }

  private def createInfoPlist() = {
    val content = s"""<?xml version="1.0" encoding="UTF-8"?>
                    |<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
                    |<plist version="1.0">
                    |<dict>
                    |	<key>CFBundleExecutable</key>
                    |	<string>$packageName</string>
                    |	<key>CFBundleIdentifier</key>
                    |	<string>com.example.yours</string>
                    |	<key>NSHighResolutionCapable</key>
                    |	<true/>
                    |	<key>LSUIElement</key>
                    |	<true/>
                    |</dict>
                    |</plist>""".stripMargin

    os.write(contentPath / "Info.plist", content)
  }

  private def createAppDirectory() = {
    os.makeDir.all(macOsPath)
    os.copy(sourceAppPath, macOsPath / packageName)
  }
}