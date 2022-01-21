package scala.build.bsp;

import org.eclipse.lsp4j.jsonrpc.validation.NonNull;
import org.eclipse.lsp4j.util.Preconditions;
import org.eclipse.xtext.xbase.lib.Pure;
import org.eclipse.xtext.xbase.lib.util.ToStringBuilder;

public class WrappedSourceItem {
  @NonNull
  private String uri;
  @NonNull
  private String generatedUri;

  private String topWrapper;
  private String bottomWrapper;

  public WrappedSourceItem(@NonNull final String uri, @NonNull final String generatedUri) {
    this.uri = uri;
    this.generatedUri = generatedUri;
  }

  @Pure
  @NonNull
  public String getUri() {
    return this.uri;
  }

  public void setUri(@NonNull final String uri) {
    this.uri = Preconditions.checkNotNull(uri, "uri");
  }

  @Pure
  @NonNull
  public String getGeneratedUri() {
    return this.generatedUri;
  }

  public void setGeneratedUri(@NonNull final String generatedUri) {
    this.generatedUri = Preconditions.checkNotNull(generatedUri, "generatedUri");
  }

  @Pure
  public String getTopWrapper() {
    return this.topWrapper;
  }

  public void setTopWrapper(final String topWrapper) {
    this.topWrapper = topWrapper;
  }

  @Pure
  public String getBottomWrapper() {
    return this.bottomWrapper;
  }

  public void setBottomWrapper(final String bottomWrapper) {
    this.bottomWrapper = bottomWrapper;
  }

  @Override
  @Pure
  public String toString() {
    ToStringBuilder b = new ToStringBuilder(this);
    b.add("uri", this.uri);
    b.add("generatedUri", this.generatedUri);
    b.add("topWrapper", this.topWrapper);
    b.add("bottomWrapper", this.bottomWrapper);
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
    WrappedSourceItem other = (WrappedSourceItem) obj;
    if (this.uri == null) {
      if (other.uri != null)
        return false;
    } else if (!this.uri.equals(other.uri))
      return false;
    if (this.generatedUri == null) {
      if (other.generatedUri != null)
        return false;
    } else if (!this.generatedUri.equals(other.generatedUri))
      return false;
    if (this.topWrapper == null) {
      if (other.topWrapper != null)
        return false;
    } else if (!this.topWrapper.equals(other.topWrapper))
      return false;
    if (this.bottomWrapper == null) {
      if (other.bottomWrapper != null)
        return false;
    } else if (!this.bottomWrapper.equals(other.bottomWrapper))
      return false;
    return true;
  }

  @Override
  @Pure
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((this.uri== null) ? 0 : this.uri.hashCode());
    result = prime * result + ((this.generatedUri== null) ? 0 : this.generatedUri.hashCode());
    result = prime * result + ((this.topWrapper== null) ? 0 : this.topWrapper.hashCode());
    return prime * result + ((this.bottomWrapper== null) ? 0 : this.bottomWrapper.hashCode());
  }
}
