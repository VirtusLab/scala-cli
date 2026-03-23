package scala.build.testrunner;

import sbt.testing.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class JavaDynamicTestRunner {

    /**
     * Based on junit-interface GlobFilter.compileGlobPattern:
     * https://github.com/sbt/junit-interface/blob/f8c6372ed01ce86f15393b890323d96afbe6d594/src/main/java/com/novocode/junit/GlobFilter.java#L37
     *
     * Converts a glob expression (only * supported) into a regex Pattern.
     */
    private static Pattern globPattern(String expr) {
        String[] parts = expr.split("\\*", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i != 0) sb.append(".*");
            if (!parts[i].isEmpty()) sb.append(Pattern.quote(parts[i].replace("\n", "\\n")));
        }
        return Pattern.compile(sb.toString());
    }

    public static void main(String[] args) {
        List<String> testFrameworks = new ArrayList<>();
        List<String> remainingArgs = new ArrayList<>();
        boolean requireTests = false;
        int verbosity = 0;
        Optional<String> testOnly = Optional.empty();

        boolean pastDashDash = false;
        for (String arg : args) {
            if (pastDashDash) {
                remainingArgs.add(arg);
            } else if ("--".equals(arg)) {
                pastDashDash = true;
            } else if (arg.startsWith("--test-framework=")) {
                testFrameworks.add(arg.substring("--test-framework=".length()));
            } else if (arg.startsWith("--test-only=")) {
                testOnly = Optional.of(arg.substring("--test-only=".length()));
            } else if (arg.startsWith("--verbosity=")) {
                try {
                    verbosity = Integer.parseInt(arg.substring("--verbosity=".length()));
                } catch (NumberFormatException e) {
                    // ignore malformed
                }
            } else if ("--require-tests".equals(arg)) {
                requireTests = true;
            } else {
                remainingArgs.add(arg);
            }
        }

        JavaTestLogger logger = new JavaTestLogger(verbosity, System.err);

        if (!testFrameworks.isEmpty()) {
            logger.debug("Directly passed " + testFrameworks.size() + " test frameworks:\n  - " +
                String.join("\n  - ", testFrameworks));
        }

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        java.util.List<java.nio.file.Path> classPath0 = JavaTestRunner.classPath(classLoader);

        List<Framework> frameworks;
        if (!testFrameworks.isEmpty()) {
            frameworks = new ArrayList<>();
            for (String fw : testFrameworks) {
                try {
                    frameworks.add(JavaFrameworkUtils.loadFramework(classLoader, fw));
                } catch (Exception e) {
                    System.err.println("Could not load test framework: " + fw);
                    System.err.println(e.getMessage());
                    System.exit(1);
                }
            }
        } else {
            List<Framework> frameworkServices = JavaFrameworkUtils.findFrameworkServices(classLoader);
            List<Framework> scannedFrameworks = JavaFrameworkUtils.findFrameworks(
                classPath0, classLoader, JavaTestRunner.commonTestFrameworks()
            );
            List<Framework> toRun = JavaFrameworkUtils.getFrameworksToRun(
                frameworkServices, scannedFrameworks, logger
            );
            if (toRun.isEmpty()) {
                if (verbosity >= 2) {
                    throw new RuntimeException("No test framework found");
                } else {
                    System.err.println("No test framework found");
                    System.exit(1);
                }
            }
            frameworks = toRun;
        }

        String[] runnerArgs = remainingArgs.toArray(new String[0]);
        final Optional<String> testOnlyFinal = testOnly;
        final boolean requireTestsFinal = requireTests;

        boolean anyFailed = false;
        for (Framework framework : frameworks) {
            logger.log("Running test framework: " + framework.name());
            Fingerprint[] fingerprints = framework.fingerprints();
            Runner runner = framework.runner(runnerArgs, new String[0], classLoader);

            List<Class<?>> classes = new ArrayList<>();
            for (String name : JavaFrameworkUtils.listClasses(classPath0, false)) {
                try {
                    classes.add(classLoader.loadClass(name));
                } catch (ClassNotFoundException | NoClassDefFoundError |
                         UnsupportedClassVersionError | IncompatibleClassChangeError e) {
                    // skip
                }
            }

            List<TaskDef> taskDefs = new ArrayList<>();
            for (Class<?> cls : classes) {
                Optional<Fingerprint> fp = JavaFrameworkUtils.matchFingerprints(
                    classLoader, cls, fingerprints
                );
                if (!fp.isPresent()) continue;
                String clsName = cls.getName().endsWith("$")
                    ? cls.getName().substring(0, cls.getName().length() - 1)
                    : cls.getName();
                if (testOnlyFinal.isPresent()) {
                    Pattern pat = globPattern(testOnlyFinal.get());
                    if (!pat.matcher(clsName).matches()) continue;
                }
                taskDefs.add(new TaskDef(clsName, fp.get(), false, new Selector[]{new SuiteSelector()}));
            }

            Task[] initialTasks = runner.tasks(taskDefs.toArray(new TaskDef[0]));
            List<Event> events = JavaTestRunner.runTasks(Arrays.asList(initialTasks), System.out);

            boolean failed = events.stream().anyMatch(ev ->
                ev.status() == Status.Error ||
                ev.status() == Status.Failure ||
                ev.status() == Status.Canceled
            );

            String doneMsg = runner.done();
            if (doneMsg != null && !doneMsg.isEmpty()) System.out.println(doneMsg);

            if (requireTestsFinal && events.isEmpty()) {
                logger.error("Error: no tests were run for " + framework.name() + ".");
                anyFailed = true;
            } else if (failed) {
                logger.error("Error: " + framework.name() + " tests failed.");
                anyFailed = true;
            } else {
                logger.log(framework.name() + " tests ran successfully.");
            }
        }

        System.exit(anyFailed ? 1 : 0);
    }
}
