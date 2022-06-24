package scala.cli.internal;

import coursier.cache.shaded.dirs.GetWinDirs;
import coursier.cache.shaded.dirs.ProjectDirectories;
import coursier.jniutils.WindowsKnownFolders;
import coursier.paths.Util;

import java.util.ArrayList;

// Copied from (https://github.com/scala-cli/bloop-core/blob/cbbe37ca36088fe314844bae5e8bf8ebbaf89060/shared/src/main/java/bloop/io/internal/ProjDirHelper.java)
// To resolve bloop cache directories used in Cache Export and Import 
public class BloopCache {
  public static String cacheDir() {
    return ((ProjectDirectories) get()).cacheDir;
  }
  // not letting ProjectDirectories leak in the signature, getting weird scalac crashes
  // with Scala 2.12.5 (because of shading?)
  private static Object get() {
    GetWinDirs getWinDirs;
    if (Util.useJni()) {
      getWinDirs = new GetWinDirs() {
        public String[] getWinDirs(String ...guids) {
          ArrayList<String> l = new ArrayList<>();
          for (int idx = 0; idx < guids.length; idx++) {
            l.add(WindowsKnownFolders.knownFolderPath("{" + guids[idx] + "}"));
          }
          return l.toArray(new String[l.size()]);
        }
      };
    } else {
      getWinDirs = GetWinDirs.powerShellBased;
    }
    return ProjectDirectories.from("", "", "bloop", getWinDirs);
  }
}
