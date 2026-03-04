package scala.cli.integration.bsp;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import java.util.List;

public class WrappedSourcesItem {
  public BuildTargetIdentifier target;
  public List<WrappedSourceItem> sources;
}
