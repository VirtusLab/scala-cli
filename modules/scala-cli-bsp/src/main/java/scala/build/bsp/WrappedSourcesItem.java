package scala.build.bsp;

import java.util.List;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;
import org.eclipse.lsp4j.util.Preconditions;
import org.eclipse.xtext.xbase.lib.Pure;
import org.eclipse.xtext.xbase.lib.util.ToStringBuilder;

public class WrappedSourcesItem {
  @NonNull
  private BuildTargetIdentifier target;

  @NonNull
  private List<WrappedSourceItem> sources;

  public WrappedSourcesItem(@NonNull final BuildTargetIdentifier target, @NonNull final List<WrappedSourceItem> sources) {
    this.target = target;
    this.sources = sources;
  }

  @Pure
  @NonNull
  public BuildTargetIdentifier getTarget() {
    return this.target;
  }

  public void setTarget(@NonNull final BuildTargetIdentifier target) {
    this.target = Preconditions.checkNotNull(target, "target");
  }

  @Pure
  @NonNull
  public List<WrappedSourceItem> getSources() {
    return this.sources;
  }

  public void setSources(@NonNull final List<WrappedSourceItem> sources) {
    this.sources = Preconditions.checkNotNull(sources, "sources");
  }

  @Override
  @Pure
  public String toString() {
    ToStringBuilder b = new ToStringBuilder(this);
    b.add("target", this.target);
    b.add("sources", this.sources);
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
    WrappedSourcesItem other = (WrappedSourcesItem) obj;
    if (this.target == null) {
      if (other.target != null)
        return false;
    } else if (!this.target.equals(other.target))
      return false;
    if (this.sources == null) {
      if (other.sources != null)
        return false;
    } else if (!this.sources.equals(other.sources))
      return false;
    return true;
  }

  @Override
  @Pure
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((this.target== null) ? 0 : this.target.hashCode());
    return prime * result + ((this.sources== null) ? 0 : this.sources.hashCode());
  }
}
