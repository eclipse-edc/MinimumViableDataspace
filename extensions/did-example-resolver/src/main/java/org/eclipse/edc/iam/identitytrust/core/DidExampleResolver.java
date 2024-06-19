package org.eclipse.edc.iam.identitytrust.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.resolution.DidResolver;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class DidExampleResolver implements DidResolver {

    private final Map<String, String> cache = new HashMap<>();
    private final ObjectMapper objectMapper;
    private final Monitor monitor;

    public DidExampleResolver(ObjectMapper objectMapper, Monitor monitor) {
        this.objectMapper = objectMapper;
        this.monitor = monitor;
    }

    @Override
    public @NotNull String getMethod() {
        return "example";
    }

    @Override
    public @NotNull Result<DidDocument> resolve(String did) {

        // chop off fragment
        var ix = did.indexOf("#");
        if (ix > 0) {
            did = did.substring(0, ix);
        }

        var resourceName = cache.get(did);
        if (resourceName == null) {
            return Result.failure("DID '%s' found found".formatted(did));
        }
        try (var stream = getClass().getClassLoader().getResourceAsStream(resourceName)) {

            var scanner = new Scanner(stream).useDelimiter("\\A");
            var content = scanner.hasNext() ? scanner.next() : "";

            var doc = objectMapper.readValue(content, DidDocument.class);
            return Result.success(doc);
        } catch (IOException e) {
            monitor.warning("Error converting did", e);
            return Result.failure("Error converting did");
        }
    }

    public void addCached(String did, String url) {
        cache.put(did, url);
    }
}
