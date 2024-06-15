package org.eclipse.edc.demo.participants.resolver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.iam.identitytrust.core.DidExampleResolver;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LazyLoadNodeDirectory implements TargetNodeDirectory {
    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {
    };
    private static final String PROTOCOL_ENDPOINT = "ProtocolEndpoint";

    private final ObjectMapper mapper;
    private final File participantListFile;
    private final DidResolverRegistry didResolverRegistry;
    private final Monitor monitor;

    public LazyLoadNodeDirectory(ObjectMapper mapper, File participantListFile, DidResolverRegistry didResolverRegistry, Monitor monitor) {

        this.mapper = mapper;
        this.participantListFile = participantListFile;
        this.didResolverRegistry = didResolverRegistry;
        this.monitor = monitor;
    }

    @Override
    public List<TargetNode> getAll() {
        try {

            var entries = mapper.readValue(participantListFile, MAP_TYPE);

            var list= entries.entrySet().stream()
                    .map(e -> createNode(e.getKey(), e.getValue()))
                    .filter(Objects::nonNull)
                    .toList();

            return list;
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public void insert(TargetNode targetNode) {
        //noop
    }

    private TargetNode createNode(String name, String did) {
        var didResult = didResolverRegistry.resolve(did);
        if (didResult.failed()) {
            monitor.warning(didResult.getFailureDetail());
            return null;
        }
        var document = didResult.getContent();
        var service = document.getService().stream().filter(s -> s.getType().equalsIgnoreCase(PROTOCOL_ENDPOINT)).findFirst();
        return service.map(s -> new TargetNode(name, did, s.getServiceEndpoint(), List.of("dataspace-protocol-http")))
                .orElse(null);
    }
}
