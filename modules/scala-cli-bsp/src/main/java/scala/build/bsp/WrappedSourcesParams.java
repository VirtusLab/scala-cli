package scala.build.bsp;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import java.util.List;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;
import org.eclipse.xtext.xbase.lib.Pure;
import org.eclipse.xtext.xbase.lib.util.ToStringBuilder;

public class WrappedSourcesParams {
  @NonNull
  private List<BuildTargetIdentifier> targets;

  public WrappedSourcesParams(@NonNull final List<BuildTargetIdentifier> targets) {
    this.targets = targets;
  }

  @Pure
  @NonNull
  public List<BuildTargetIdentifier> getTargets() {
    return this.targets;
  }

  @Override
  @Pure
  public String toString() {
    ToStringBuilder b = new ToStringBuilder(this);
    b.add("targets", this.targets);
    return b.toString();
  }

  @Override
  @Pure
  public boolean equals(final Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    WrappedSourcesParams other = (WrappedSourcesParams) obj;
    if (this.targets == null) {
      if (other.targets != null)
        return false;
    } else if (!this.targets.equals(other.targets))
      return false;
    return true;
  }

  @Override
  @Pure
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    return prime * result + ((this.targets== null) ? 0 : this.targets.hashCode());
  }
}
