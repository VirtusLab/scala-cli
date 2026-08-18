package scala.build.internal

import org.objectweb.asm
import org.objectweb.asm.ClassReader

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.file.NoSuchFileException
import java.util.jar.{Attributes, JarFile}

import scala.build.internal.zip.WrappedZipInputStream
import scala.build.{Logger, retry}

object MainClass {

  /** Shape of a `main` entry point recognised in bytecode.
    *
    * Case declaration order is the JEP 512 resolution order: `main(String[])` before `main()`, and
    * within each signature the static form before the instance form.
    *
    * Hierarchy is resolved within the scanned classpath entry: a `main` declared on a superclass or
    * Java interface in that entry is visible on concrete subclasses. Static methods are not
    * inherited from interfaces. A private `main` on a nearer class hides a `main` of the same
    * parameter signature further up the superclass chain; the search does not continue past it. A
    * private declaration does not hide a `main` of the other signature. Ancestors that live outside
    * the scanned entry (dependency JARs, the JDK) are still unresolved. Scala traits remain a
    * special case: the compiler emits a mixin forwarder into the implementing class, so that class
    * declares `main` itself.
    */
  enum MainMethodKind(val requiresJep512: Boolean, val isStatic: Boolean):
    case StaticWithArgs          extends MainMethodKind(false, true)
    case NonPublicStaticWithArgs extends MainMethodKind(true, true)
    case InstanceWithArgs        extends MainMethodKind(true, false)
    case StaticNoArgs            extends MainMethodKind(true, true)
    case InstanceNoArgs          extends MainMethodKind(true, false)

    /** Whether a JVM of version `jvmVersion` can launch this main method shape. The JEP 512 shapes
      * need JDK 25 or newer, or JDK 21 or newer with `--enable-preview`.
      */
    def isSupportedByJvm(jvmVersion: Int, previewEnabled: Boolean): Boolean =
      !requiresJep512 ||
      jvmVersion >= Constants.jep512MinJavaVersion ||
      (previewEnabled && jvmVersion >= Constants.jep512PreviewMinJavaVersion)

  final case class MainClassCandidate(className: String, kind: MainMethodKind)

  private val stringArrayDescriptor = "([Ljava/lang/String;)V"
  private val noArgDescriptor       = "()V"

  private final case class ClassInfo(
    className: String,
    superClassOpt: Option[String],
    interfaces: Seq[String],
    isInstantiable: Boolean,
    hasNonPrivateNoArgCtor: Boolean,
    declaredMains: Map[Boolean, Option[MainMethodKind]]
  )

  private class MainMethodChecker extends asm.ClassVisitor(asm.Opcodes.ASM9) {
    private var nameOpt: Option[String]                             = None
    private var superClassOpt: Option[String]                       = None
    private var interfaces: Seq[String]                             = Nil
    private var classAccess: Int                                    = 0
    private var hasNonPrivateNoArgCtor: Boolean                     = false
    private var declaredMains: Map[Boolean, Option[MainMethodKind]] = Map.empty

    private def recordMain(
      hasArgs: Boolean,
      kindOpt: Option[MainMethodKind]
    ): Unit =
      declaredMains.get(hasArgs) match {
        case None                 => declaredMains += hasArgs -> kindOpt
        case Some(None)           => kindOpt.foreach(kind => declaredMains += hasArgs -> Some(kind))
        case Some(Some(existing)) =>
          for kind <- kindOpt do
            val best = MainMethodKind.values.find(k => k == existing || k == kind).get
            declaredMains += hasArgs -> Some(best)
      }

    private def dotted(internalName: String): String =
      internalName.replace('/', '.').replace('\\', '.')

    override def visit(
      version: Int,
      access: Int,
      name: String,
      signature: String,
      superName: String,
      interfaces: Array[String]
    ): Unit = {
      classAccess = access
      nameOpt = Some(dotted(name))
      superClassOpt = Option(superName).map(dotted)
      this.interfaces = Option(interfaces).toSeq.flatten.map(dotted)
    }

    override def visitMethod(
      access: Int,
      name: String,
      descriptor: String,
      signature: String,
      exceptions: Array[String]
    ): asm.MethodVisitor = {
      import MainMethodKind.*
      val isStatic  = (access & asm.Opcodes.ACC_STATIC) != 0
      val isPrivate = (access & asm.Opcodes.ACC_PRIVATE) != 0
      val isPublic  = (access & asm.Opcodes.ACC_PUBLIC) != 0
      if name == "<init>" && descriptor == noArgDescriptor && !isPrivate then
        hasNonPrivateNoArgCtor = true
      else if name == "main" then
        val hasArgsOpt = descriptor match {
          case `stringArrayDescriptor` => Some(true)
          case `noArgDescriptor`       => Some(false)
          case _                       => None
        }
        for hasArgs <- hasArgsOpt do
          val kindOpt = Option.unless(isPrivate) {
            (hasArgs, isStatic, isPublic) match {
              case (true, true, true)  => StaticWithArgs
              case (true, true, false) => NonPublicStaticWithArgs
              case (true, false, _)    => InstanceWithArgs
              case (false, true, _)    => StaticNoArgs
              case (false, false, _)   => InstanceNoArgs
            }
          }
          recordMain(hasArgs, kindOpt)
      null
    }

    def classInfoOpt: Option[ClassInfo] =
      nameOpt.map { className =>
        val isAbstractOrInterface = (classAccess & asm.Opcodes.ACC_ABSTRACT) != 0 ||
          (classAccess & asm.Opcodes.ACC_INTERFACE) != 0
        ClassInfo(
          className = className,
          superClassOpt = superClassOpt,
          interfaces = interfaces,
          isInstantiable = !isAbstractOrInterface,
          hasNonPrivateNoArgCtor = hasNonPrivateNoArgCtor,
          declaredMains = declaredMains
        )
      }
  }

  private def candidates(classInfos: Seq[ClassInfo]): Seq[MainClassCandidate] = {
    val byName = classInfos.map(info => info.className -> info).toMap

    def fromClassChain(
      className: String,
      hasArgs: Boolean,
      seen: Set[String]
    ): Option[Option[MainMethodKind]] =
      if seen.contains(className) then None
      else
        byName.get(className).flatMap { info =>
          info.declaredMains.get(hasArgs).orElse(
            info.superClassOpt.flatMap(fromClassChain(_, hasArgs, seen + className))
          )
        }

    def fromInterfaces(
      className: String,
      hasArgs: Boolean,
      seen: Set[String]
    ): Option[MainMethodKind] =
      if seen.contains(className) then None
      else
        byName.get(className).flatMap { info =>
          val seen0 = seen + className
          info.declaredMains.get(hasArgs).flatten.filterNot(_.isStatic).orElse {
            info.interfaces.view.flatMap(fromInterfaces(_, hasArgs, seen0)).headOption
              .orElse(info.superClassOpt.flatMap(fromInterfaces(_, hasArgs, seen0)))
          }
        }

    def visibleKind(className: String, hasArgs: Boolean): Option[MainMethodKind] =
      fromClassChain(className, hasArgs, Set.empty) match {
        case Some(kindOpt) => kindOpt
        case None          => fromInterfaces(className, hasArgs, Set.empty)
      }

    classInfos.filter(_.isInstantiable).flatMap { info =>
      // constructors are not inherited, so instance shapes depend on this class's own constructor
      val invocableKinds = Seq(true, false)
        .flatMap(visibleKind(info.className, _))
        .filter(kind => kind.isStatic || info.hasNonPrivateNoArgCtor)
        .toSet
      MainMethodKind.values.find(invocableKinds.contains)
        .map(MainClassCandidate(info.className, _))
    }
  }

  private def findInClass(path: os.Path, logger: Logger): Iterator[ClassInfo] =
    try {
      val is = retry()(logger)(os.read.inputStream(path))
      findInClass(is, logger)
    }
    catch {
      case e: NoSuchFileException =>
        e.getStackTrace.foreach(ste => logger.debug(ste.toString))
        logger.log(s"Class file $path not found: $e")
        logger.log("Are you trying to run too many builds at once? Trying to recover...")
        Iterator.empty
    }

  private def findInClass(is: InputStream, logger: Logger): Iterator[ClassInfo] =
    try retry()(logger) {
        val reader  = new ClassReader(is)
        val checker = new MainMethodChecker
        reader.accept(checker, 0)
        checker.classInfoOpt.iterator
      }
    catch {
      case e: ArrayIndexOutOfBoundsException =>
        e.getStackTrace.foreach(ste => logger.debug(ste.toString))
        logger.log(s"Class input stream could not be created: $e")
        logger.log("Are you trying to run too many builds at once? Trying to recover...")
        Iterator.empty
      case e: IllegalArgumentException =>
        e.getStackTrace.foreach(ste => logger.debug(ste.toString))
        logger.log(s"Class file could not be read (unsupported class file version?): $e")
        Iterator.empty
    }
    finally is.close()

  private def findInJar(path: os.Path, logger: Logger): Iterator[ClassInfo] =
    try retry()(logger) {
        val content        = os.read.bytes(path)
        val jarInputStream = WrappedZipInputStream.create(new ByteArrayInputStream(content))
        jarInputStream.entries().flatMap(ent =>
          if !ent.isDirectory && ent.getName.endsWith(".class") then {
            val content     = jarInputStream.readAllBytes()
            val inputStream = new ByteArrayInputStream(content)
            findInClass(inputStream, logger)
          }
          else Iterator.empty
        )
      }
    catch {
      case e: NoSuchFileException =>
        logger.debugStackTrace(e)
        logger.log(s"JAR file $path not found: $e, trying to recover...")
        logger.log("Are you trying to run too many builds at once? Trying to recover...")
        Iterator.empty
    }

  def findInDependency(jar: os.Path): Option[String] =
    jar match {
      case jar if os.isFile(jar) && jar.last.endsWith(".jar") =>
        for {
          manifest          <- Option(new JarFile(jar.toIO).getManifest)
          mainAttributes    <- Option(manifest.getMainAttributes)
          mainClass: String <- Option(mainAttributes.getValue(Attributes.Name.MAIN_CLASS))
        } yield mainClass
      case _ => None
    }

  def find(output: os.Path, logger: Logger): Seq[MainClassCandidate] = {
    val classInfos: Seq[ClassInfo] = output match {
      case o if os.isFile(o) && o.last.endsWith(".class") =>
        findInClass(o, logger).toVector
      case o if os.isFile(o) && o.last.endsWith(".jar") =>
        findInJar(o, logger).toVector
      case o if os.isDir(o) =>
        os.walk(o)
          .iterator
          .filter(os.isFile)
          .flatMap {
            case classFilePath if classFilePath.last.endsWith(".class") =>
              findInClass(classFilePath, logger)
            case _ => Iterator.empty
          }
          .toVector
      case _ => Vector.empty
    }
    candidates(classInfos)
  }
}
