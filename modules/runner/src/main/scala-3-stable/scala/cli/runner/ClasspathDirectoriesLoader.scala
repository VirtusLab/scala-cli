package scala.cli.runner

import java.net.URLClassLoader
import java.net.URL
import java.io.File

import org.virtuslab.stacktraces.io.Unzipper
import org.virtuslab.stacktraces.model.ClasspathWrapper

object ClasspathDirectoriesLoader:

  private def getUrls(cl: ClassLoader): Array[File] = cl match
    case null              => Array()
    case u: URLClassLoader => u.getURLs.map(u => new File(u.toURI)) ++ getUrls(cl.getParent)
    case cl if cl.getClass.getName == "jdk.internal.loader.ClassLoaders$AppClassLoader" =>
      // Required with JDK-11
      sys.props.getOrElse("java.class.path", "")
        .split(File.pathSeparator)
        .filter(_.nonEmpty)
        .map(new File(_))
    case _ => getUrls(cl.getParent)

  def getClasspath(loader: ClassLoader = Thread.currentThread().getContextClassLoader): List[File] =
    getUrls(loader).toList

  def getClasspathDirectories(classPathFiles: List[File] = getClasspath()): List[ClasspathWrapper] =
    val (directories, jars) = classPathFiles.partition(_.isDirectory)
    val allDirectories = projectClassesToClasspathWrappers(directories) ++
      jarsToClasspathWrappers(jars)
    allDirectories

  private def projectClassesToClasspathWrappers(directories: List[File]): List[ClasspathWrapper] =
    directories.map(ClasspathWrapper(_, None))

  private def jarsToClasspathWrappers(jars: List[File]): List[ClasspathWrapper] =
    jars.map(j => ClasspathWrapper(Unzipper.unzipFile(j), Some(j.getName)))
