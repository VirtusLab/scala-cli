package scala.cli.internal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

// Remove once we can use https://github.com/com-lihaoyi/PPrint/pull/80

@TargetClass(className = "pprint.StringPrefix$")
final class PPrintStringPrefixSubst {

  @Substitute
  String apply(scala.collection.Iterable<?> i) {
    String name = (new PPrintStringPrefixHelper()).apply((scala.collection.Iterable<Object>) i);
    return name;
  }

}
