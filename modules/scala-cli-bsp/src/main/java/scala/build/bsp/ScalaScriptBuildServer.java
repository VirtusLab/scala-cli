package scala.build.bsp;

import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;

import java.util.concurrent.CompletableFuture;

public interface ScalaScriptBuildServer {

    @JsonRequest("buildTarget/wrappedSources")
    CompletableFuture<WrappedSourcesResult> buildTargetWrappedSources(WrappedSourcesParams params);

}
