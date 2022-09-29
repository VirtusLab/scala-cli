package scala.cli.config;

import java.nio.file.Path;

import scala.cli.config.internal.JavaHelper;

public final class JavaInterface {

    public static void open(Path dbPath) {
      JavaHelper.open(dbPath);
    }

    public static void close() {
      JavaHelper.close();
    }

    public static String getString(String key) {
      return JavaHelper.getString(key);
    }

    public static Boolean getBoolean(String key) {
      return JavaHelper.getBoolean(key);
    }

    public static String[] getStringList(String key) {
      return JavaHelper.getStringList(key);
    }

    public static String getPassword(String key) {
      return JavaHelper.getPassword(key);
    }

    public static byte[] getPasswordBytes(String key) {
      return JavaHelper.getPasswordBytes(key);
    }

}
