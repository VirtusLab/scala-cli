package scala.build.testrunner;

import java.io.PrintStream;

public class JavaTestLogger {
    private final int verbosity;
    private final PrintStream out;

    public JavaTestLogger(int verbosity, PrintStream out) {
        this.verbosity = verbosity;
        this.out = out;
    }

    public void error(String message) {
        out.println(message);
    }

    public void message(String message) {
        if (verbosity >= 0) out.println(message);
    }

    public void log(String message) {
        if (verbosity >= 1) out.println(message);
    }

    public void debug(String message) {
        if (verbosity >= 2) out.println(message);
    }
}
