package scala.build.testrunner;

import sbt.testing.*;

import java.io.File;
import java.io.PrintStream;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

public class JavaTestRunner {

    public static List<String> commonTestFrameworks() {
        return Arrays.asList(
            "munit.Framework",
            "utest.runner.Framework",
            "org.scalacheck.ScalaCheckFramework",
            "zio.test.sbt.ZTestFramework",
            "org.scalatest.tools.Framework",
            "com.novocode.junit.JUnitFramework",
            "org.scalajs.junit.JUnitFramework",
            "weaver.framework.CatsEffect"
        );
    }

    public static List<Path> classPath(ClassLoader loader) {
        List<Path> result = new ArrayList<>();
        collectClassPath(loader, result);
        return result;
    }

    private static void collectClassPath(ClassLoader loader, List<Path> result) {
        if (loader == null) return;
        if (loader instanceof URLClassLoader) {
            URLClassLoader urlLoader = (URLClassLoader) loader;
            for (java.net.URL url : urlLoader.getURLs()) {
                if ("file".equals(url.getProtocol())) {
                    try {
                        result.add(Paths.get(url.toURI()).toAbsolutePath());
                    } catch (Exception e) {
                        // skip
                    }
                }
            }
        } else if (loader.getClass().getName().equals("jdk.internal.loader.ClassLoaders$AppClassLoader")) {
            String cp = System.getProperty("java.class.path", "");
            for (String entry : cp.split(File.pathSeparator)) {
                if (!entry.isEmpty()) {
                    result.add(Paths.get(entry));
                }
            }
        }
        collectClassPath(loader.getParent(), result);
    }

    public static List<Event> runTasks(List<Task> initialTasks, PrintStream out) {
        Deque<Task> tasks = new ArrayDeque<>(initialTasks);
        List<Event> events = new ArrayList<>();

        sbt.testing.Logger logger = new sbt.testing.Logger() {
            public boolean ansiCodesSupported() { return true; }
            public void error(String msg) { out.println(msg); }
            public void warn(String msg) { out.println(msg); }
            public void info(String msg) { out.println(msg); }
            public void debug(String msg) { out.println(msg); }
            public void trace(Throwable t) { t.printStackTrace(out); }
        };

        EventHandler eventHandler = event -> events.add(event);
        sbt.testing.Logger[] loggers = new sbt.testing.Logger[]{logger};

        while (!tasks.isEmpty()) {
            Task task = tasks.poll();
            Task[] newTasks = task.execute(eventHandler, loggers);
            for (Task t : newTasks) {
                tasks.add(t);
            }
        }

        return events;
    }
}
