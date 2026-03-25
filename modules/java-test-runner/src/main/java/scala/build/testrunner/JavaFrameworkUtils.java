package scala.build.testrunner;

import sbt.testing.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.stream.Stream;

public class JavaFrameworkUtils {

    public static List<Framework> findFrameworkServices(ClassLoader loader) {
        List<Framework> result = new ArrayList<>();
        ServiceLoader<Framework> serviceLoader = ServiceLoader.load(Framework.class, loader);
        for (Framework f : serviceLoader) {
            result.add(f);
        }
        return result;
    }

    public static Framework loadFramework(ClassLoader loader, String className) throws Exception {
        Class<?> cls = loader.loadClass(className);
        return (Framework) cls.getConstructor().newInstance();
    }

    public static List<Framework> findFrameworks(
        List<Path> classPath,
        ClassLoader loader,
        List<String> preferredClasses,
        JavaTestLogger logger
    ) {
        Class<?> frameworkCls = Framework.class;
        List<Framework> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        // first try preferred classes, then scan classpath
        List<String> candidates = new ArrayList<>(preferredClasses);
        for (String name : listClasses(classPath, true, logger)) {
            if (!seen.contains(name)) {
                candidates.add(name);
            }
        }

        for (String name : candidates) {
            if (!seen.add(name)) continue;
            Class<?> cls;
            try {
                cls = loader.loadClass(name);
            } catch (ClassNotFoundException | UnsupportedClassVersionError |
                     NoClassDefFoundError | IncompatibleClassChangeError e) {
                // Expected: most classpath entries aren't test frameworks
                continue;
            }
            if (!frameworkCls.isAssignableFrom(cls)) continue;
            if (Modifier.isAbstract(cls.getModifiers())) continue;
            long publicNoArgCtors = Arrays.stream(cls.getConstructors())
                .filter(c -> Modifier.isPublic(c.getModifiers()) && c.getParameterCount() == 0)
                .count();
            if (publicNoArgCtors != 1) continue;
            try {
                Framework instance = (Framework) cls.getConstructor().newInstance();
                result.add(instance);
            } catch (Exception e) {
                logger.debug("Could not instantiate framework " + name + ": " + e);
            }
        }
        return result;
    }

    public static Optional<Fingerprint> matchFingerprints(
        ClassLoader loader,
        Class<?> cls,
        Fingerprint[] fingerprints,
        JavaTestLogger logger
    ) {
        boolean isModule = cls.getName().endsWith("$");
        long publicCtorCount = Arrays.stream(cls.getConstructors())
            .filter(c -> Modifier.isPublic(c.getModifiers()))
            .count();
        boolean noPublicConstructors = publicCtorCount == 0;
        boolean definitelyNoTests = Modifier.isAbstract(cls.getModifiers()) ||
            cls.isInterface() ||
            publicCtorCount > 1 ||
            isModule != noPublicConstructors;
        if (definitelyNoTests) return Optional.empty();

        for (Fingerprint fp : fingerprints) {
            if (fp instanceof SubclassFingerprint) {
                SubclassFingerprint sf = (SubclassFingerprint) fp;
                if (sf.isModule() != isModule) continue;
                try {
                    Class<?> superCls = loader.loadClass(sf.superclassName());
                    if (superCls.isAssignableFrom(cls)) return Optional.of(fp);
                } catch (ClassNotFoundException e) {
                    logger.debug(
                        "Superclass not found for fingerprint matching: " + sf.superclassName());
                }
            } else if (fp instanceof AnnotatedFingerprint) {
                AnnotatedFingerprint af = (AnnotatedFingerprint) fp;
                if (af.isModule() != isModule) continue;
                try {
                    @SuppressWarnings("unchecked")
                    Class<? extends Annotation> annotationCls =
                        (Class<? extends Annotation>) loader.loadClass(af.annotationName());
                    boolean matches =
                        cls.isAnnotationPresent(annotationCls) ||
                        Arrays.stream(cls.getDeclaredMethods())
                            .anyMatch(m -> m.isAnnotationPresent(annotationCls)) ||
                        Arrays.stream(cls.getMethods())
                            .anyMatch(m -> m.isAnnotationPresent(annotationCls) &&
                                          Modifier.isPublic(m.getModifiers()));
                    if (matches) return Optional.of(fp);
                } catch (ClassNotFoundException e) {
                    logger.debug(
                        "Annotation class not found for fingerprint matching: " + af.annotationName());
                }
            }
        }
        return Optional.empty();
    }

    public static List<Framework> getFrameworksToRun(
        List<Framework> frameworkServices,
        List<Framework> frameworks,
        JavaTestLogger logger
    ) {
        List<Framework> all = new ArrayList<>(frameworkServices);
        all.addAll(frameworks);
        return getFrameworksToRun(all, logger);
    }

    public static List<Framework> getFrameworksToRun(
        List<Framework> allFrameworks,
        JavaTestLogger logger
    ) {
        // dedup by name
        Map<String, Framework> byName = new LinkedHashMap<>();
        for (Framework f : allFrameworks) {
            byName.putIfAbsent(f.name(), f);
        }
        List<Framework> distinct = new ArrayList<>(byName.values());

        // filter out frameworks that are superclasses of another framework in the list
        List<Framework> finalFrameworks = new ArrayList<>();
        for (Framework f1 : distinct) {
            boolean isInherited = distinct.stream()
                .filter(f2 -> f2 != f1)
                .anyMatch(f2 -> f1.getClass().isAssignableFrom(f2.getClass()));
            if (!isInherited) finalFrameworks.add(f1);
        }
        return finalFrameworks;
    }

    public static List<String> listClasses(List<Path> classPath, boolean keepJars, JavaTestLogger logger) {
        List<String> result = new ArrayList<>();
        for (Path entry : classPath) {
            result.addAll(listClasses(entry, keepJars, logger));
        }
        return result;
    }

    public static List<String> listClasses(Path entry, boolean keepJars, JavaTestLogger logger) {
        List<String> result = new ArrayList<>();
        if (Files.isDirectory(entry)) {
            try (Stream<Path> stream = Files.walk(entry, Integer.MAX_VALUE)) {
                stream.filter(p -> p.getFileName().toString().endsWith(".class"))
                    .map(entry::relativize)
                    .map(p -> {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < p.getNameCount(); i++) {
                            if (i > 0) sb.append(".");
                            sb.append(p.getName(i).toString());
                        }
                        String name = sb.toString();
                        return name.endsWith(".class") ? name.substring(0, name.length() - 6) : name;
                    })
                    .forEach(result::add);
            } catch (Exception e) {
                logger.debug("Could not walk directory " + entry + ": " + e.getMessage());
            }
        } else if (keepJars && Files.isRegularFile(entry)) {
            try (ZipFile zf = new ZipFile(entry.toFile())) {
                Enumeration<? extends ZipEntry> entries = zf.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry ze = entries.nextElement();
                    String name = ze.getName();
                    if (name.endsWith(".class")) {
                        result.add(name.substring(0, name.length() - 6).replace("/", "."));
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not read JAR " + entry + ": " + e.getMessage());
            }
        }
        return result;
    }
}
