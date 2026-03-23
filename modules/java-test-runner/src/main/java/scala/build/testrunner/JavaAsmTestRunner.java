package scala.build.testrunner;

import org.objectweb.asm.*;
import sbt.testing.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.zip.*;

public class JavaAsmTestRunner {

    public static class ParentInspector {
        private final List<Path> classPath;
        private final ConcurrentHashMap<String, List<String>> cache = new ConcurrentHashMap<>();

        public ParentInspector(List<Path> classPath) {
            this.classPath = classPath;
        }

        private List<String> parents(String className) {
            return cache.computeIfAbsent(className, name -> {
                byte[] byteCode = findInClassPath(classPath, name + ".class");
                if (byteCode == null) return Collections.emptyList();
                TestClassChecker checker = new TestClassChecker();
                ClassReader reader = new ClassReader(byteCode);
                reader.accept(checker, 0);
                return checker.getImplements();
            });
        }

        public List<String> allParents(String className) {
            List<String> result = new ArrayList<>();
            Set<String> done = new HashSet<>();
            Deque<String> todo = new ArrayDeque<>();
            todo.add(className);
            while (!todo.isEmpty()) {
                String current = todo.poll();
                if (!done.add(current)) continue;
                result.add(current);
                todo.addAll(parents(current));
            }
            return result;
        }
    }

    public static Optional<Fingerprint> matchFingerprints(
        String className,
        InputStream byteCodeStream,
        List<Fingerprint> fingerprints,
        ParentInspector parentInspector,
        ClassLoader loader
    ) throws IOException {
        TestClassChecker checker = new TestClassChecker();
        ClassReader reader = new ClassReader(byteCodeStream);
        reader.accept(checker, 0);

        boolean isModule = className.endsWith("$");
        boolean hasPublicConstructors = checker.getPublicConstructorCount() > 0;
        boolean definitelyNoTests = checker.isAbstract() ||
            checker.isInterface() ||
            checker.getPublicConstructorCount() > 1 ||
            isModule == hasPublicConstructors;

        if (definitelyNoTests) return Optional.empty();

        for (Fingerprint fp : fingerprints) {
            if (fp instanceof SubclassFingerprint) {
                SubclassFingerprint sf = (SubclassFingerprint) fp;
                if (sf.isModule() != isModule) continue;
                String superName = sf.superclassName().replace('.', '/');
                if (parentInspector.allParents(checker.getName()).contains(superName)) {
                    return Optional.of(fp);
                }
            } else if (fp instanceof AnnotatedFingerprint) {
                AnnotatedFingerprint af = (AnnotatedFingerprint) fp;
                if (af.isModule() != isModule) continue;
                // Use classloader-based reflection for annotation matching (proven approach)
                if (loader != null) {
                    try {
                        String rawName = className.replace('/', '.').replace('\\', '.');
                        String clsNameForLoad = rawName.endsWith("$") ? rawName.substring(0, rawName.length() - 1) : rawName;
                        Class<?> cls = loader.loadClass(clsNameForLoad);
                        Optional<Fingerprint> result =
                            JavaFrameworkUtils.matchFingerprints(loader, cls, new Fingerprint[]{fp});
                        if (result.isPresent()) return Optional.of(fp);
                    } catch (ClassNotFoundException | NoClassDefFoundError |
                             UnsupportedClassVersionError | IncompatibleClassChangeError e) {
                        // fall through
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static List<String> findFrameworkServices(List<Path> classPath) {
        List<String> result = new ArrayList<>();
        byte[] content = findInClassPath(classPath, "META-INF/services/sbt.testing.Framework");
        if (content != null) {
            parseServiceFileContent(new String(content, StandardCharsets.UTF_8), result);
        }
        return result;
    }

    private static void parseServiceFileContent(String content, List<String> result) {
        for (String line : content.split("[\r\n]+")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                result.add(trimmed);
            }
        }
    }

    public static List<String> findFrameworks(
        List<Path> classPath,
        List<String> preferredClasses,
        ParentInspector parentInspector
    ) {
        List<String> result = new ArrayList<>();
        // first check preferred classes
        for (String preferred : preferredClasses) {
            String resourceName = preferred.replace('.', '/') + ".class";
            byte[] bytes = findInClassPath(classPath, resourceName);
            if (bytes != null) {
                TestClassChecker checker = new TestClassChecker();
                new ClassReader(bytes).accept(checker, 0);
                if (!checker.isAbstract() && checker.getPublicConstructorCount() == 1) {
                    String internalName = preferred.replace('.', '/');
                    if (parentInspector.allParents(internalName).contains("sbt/testing/Framework")) {
                        result.add(internalName);
                    }
                }
            }
        }
        if (!result.isEmpty()) return result;

        // scan all classes in classpath
        for (Map.Entry<String, byte[]> entry : listClassesByteCode(classPath, true).entrySet()) {
            String name = entry.getKey();
            if (name.contains("module-info")) continue;
            TestClassChecker checker = new TestClassChecker();
            new ClassReader(entry.getValue()).accept(checker, 0);
            if (!checker.isAbstract() && checker.getPublicConstructorCount() == 1) {
                if (parentInspector.allParents(name).contains("sbt/testing/Framework")) {
                    result.add(name);
                }
            }
        }
        return result;
    }

    public static List<TaskDef> taskDefs(
        List<Path> classPath,
        boolean keepJars,
        List<Fingerprint> fingerprints,
        ParentInspector parentInspector,
        ClassLoader loader
    ) {
        List<TaskDef> result = new ArrayList<>();
        for (Map.Entry<String, byte[]> entry : listClassesByteCode(classPath, keepJars).entrySet()) {
            String name = entry.getKey();
            if (name.contains("module-info")) continue;
            try {
                Optional<Fingerprint> fp = matchFingerprints(
                    name,
                    new ByteArrayInputStream(entry.getValue()),
                    fingerprints,
                    parentInspector,
                    loader
                );
                if (fp.isPresent()) {
                    String stripped = name.endsWith("$") ? name.substring(0, name.length() - 1) : name;
                    String clsName = stripped.replace('/', '.').replace('\\', '.');
                    result.add(new TaskDef(clsName, fp.get(), false, new Selector[]{new SuiteSelector()}));
                }
            } catch (IOException e) {
                // skip
            }
        }
        return result;
    }

    private static Map<String, byte[]> listClassesByteCode(List<Path> classPath, boolean keepJars) {
        Map<String, byte[]> result = new LinkedHashMap<>();
        for (Path entry : classPath) {
            result.putAll(listClassesByteCode(entry, keepJars));
        }
        return result;
    }

    private static Map<String, byte[]> listClassesByteCode(Path entry, boolean keepJars) {
        Map<String, byte[]> result = new LinkedHashMap<>();
        if (Files.isDirectory(entry)) {
            try (Stream<Path> stream = Files.walk(entry, Integer.MAX_VALUE)) {
                stream.filter(p -> p.getFileName().toString().endsWith(".class"))
                    .forEach(p -> {
                        String rel = entry.relativize(p).toString().replace('\\', '/');
                        String name = rel.endsWith(".class") ? rel.substring(0, rel.length() - 6) : rel;
                        try {
                            result.put(name, Files.readAllBytes(p));
                        } catch (IOException e) {
                            // skip
                        }
                    });
            } catch (IOException e) {
                // skip
            }
        } else if (keepJars && Files.isRegularFile(entry)) {
            byte[] buf = new byte[16384];
            try (ZipFile zf = new ZipFile(entry.toFile())) {
                Enumeration<? extends ZipEntry> entries = zf.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry ze = entries.nextElement();
                    if (!ze.getName().endsWith(".class")) continue;
                    String name = ze.getName();
                    name = name.substring(0, name.length() - 6);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try (InputStream is = zf.getInputStream(ze)) {
                        int read;
                        while ((read = is.read(buf)) >= 0) {
                            baos.write(buf, 0, read);
                        }
                    }
                    result.put(name, baos.toByteArray());
                }
            } catch (IOException e) {
                // skip
            }
        }
        return result;
    }

    private static byte[] findInClassPath(List<Path> classPath, String name) {
        for (Path entry : classPath) {
            byte[] found = findInClassPathEntry(entry, name);
            if (found != null) return found;
        }
        return null;
    }

    private static byte[] findInClassPathEntry(Path entry, String name) {
        if (Files.isDirectory(entry)) {
            Path p = entry.resolve(name);
            if (Files.isRegularFile(p)) {
                try {
                    return Files.readAllBytes(p);
                } catch (IOException e) {
                    return null;
                }
            }
        } else if (Files.isRegularFile(entry)) {
            byte[] buf = new byte[16384];
            try (ZipFile zf = new ZipFile(entry.toFile())) {
                ZipEntry ze = zf.getEntry(name);
                if (ze == null) return null;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (InputStream is = zf.getInputStream(ze)) {
                    int read;
                    while ((read = is.read(buf)) >= 0) {
                        baos.write(buf, 0, read);
                    }
                }
                return baos.toByteArray();
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    public static class TestClassChecker extends ClassVisitor {
        private String name;
        private int publicConstructorCount = 0;
        private boolean isInterface = false;
        private boolean isAbstract = false;
        private List<String> implementsList = new ArrayList<>();

        public TestClassChecker() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            this.name = name;
            this.isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
            this.isAbstract = (access & Opcodes.ACC_ABSTRACT) != 0;
            if (superName != null) implementsList.add(superName);
            if (interfaces != null) {
                for (String iface : interfaces) {
                    implementsList.add(iface);
                }
            }
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            if ("<init>".equals(name) && (access & Opcodes.ACC_PUBLIC) != 0) {
                publicConstructorCount++;
            }
            return null;
        }

        public String getName() { return name; }
        public int getPublicConstructorCount() { return publicConstructorCount; }
        public boolean isInterface() { return isInterface; }
        public boolean isAbstract() { return isAbstract; }
        public List<String> getImplements() { return implementsList; }
    }
}
