package scala.build.internal

import java.math.BigInteger
import java.security.MessageDigest

import scala.build.Build
import scala.scalanative.{build => sn}

case object NativeBuilderHelper {

  private def resolveProjectShaPath(nativeWorkDir: os.Path) = nativeWorkDir / ".project_sha"
  private def resolveOutputShaPath(nativeWorkDir: os.Path)  = nativeWorkDir / ".output_sha"

  private def fileSha(filePath: os.Path): String = {
    val md = MessageDigest.getInstance("SHA-1")
    md.update(os.read.bytes(filePath))

    val digest        = md.digest()
    val calculatedSum = new BigInteger(1, digest)
    String.format(s"%040x", calculatedSum)
  }

  private def projectSha(build: Build.Successful, nativeConfig: sn.NativeConfig) = {
    val md = MessageDigest.getInstance("SHA-1")
    md.update(build.inputs.sourceHash().getBytes)
    md.update(nativeConfig.toString.getBytes)
    md.update(build.options.hash.getOrElse("").getBytes)

    val digest        = md.digest()
    val calculatedSum = new BigInteger(1, digest)
    String.format(s"%040x", calculatedSum)
  }

  def updateOutputSha(dest: os.Path, nativeWorkDir: os.Path) = {
    val outputShaPath = resolveOutputShaPath(nativeWorkDir)
    val sha           = fileSha(dest)
    os.write.over(outputShaPath, sha)
  }

  def shouldBuildIfChanged(
    build: Build.Successful,
    nativeConfig: sn.NativeConfig,
    dest: os.Path,
    nativeWorkDir: os.Path
  ): Boolean = {
    val projectShaPath = resolveProjectShaPath(nativeWorkDir)
    val outputShaPath  = resolveOutputShaPath(nativeWorkDir)

    val currentProjectSha = projectSha(build, nativeConfig)
    val currentOutputSha  = if (os.exists(dest)) Some(fileSha(dest)) else None

    val previousProjectSha = if (os.exists(projectShaPath)) Some(os.read(projectShaPath)) else None
    val previousOutputSha  = if (os.exists(outputShaPath)) Some(os.read(outputShaPath)) else None

    val changed =
      !previousProjectSha.contains(currentProjectSha) ||
      previousOutputSha != currentOutputSha ||
      !os.exists(dest)

    // update sha in .projectShaPath
    if (changed) os.write.over(projectShaPath, currentProjectSha, createFolders = true)

    changed
  }
}
