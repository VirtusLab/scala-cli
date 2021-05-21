import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

public class Hello {
  private static void printCp(ClassLoader cl) {
    if (cl == null) return;
    if (cl instanceof URLClassLoader) {
      URLClassLoader ucl = (URLClassLoader) cl;
      for (URL url : ucl.getURLs()) {
        System.out.println(url);
      }
    } else if (cl.getClass().getName() == "jdk.internal.loader.ClassLoaders$AppClassLoader") {
      String cp = System.getProperty("java.class.path");
      for (String elem : cp.split(File.pathSeparator)) {
        System.out.println(elem);
      }
    } else {
      System.out.println("* " + cl);
    }
    printCp(cl.getParent());
  }
  public static void main(String[] args) {
    System.out.println("Hello from Java");
    printCp(Thread.currentThread().getContextClassLoader());
  }
}
