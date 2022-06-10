package org.eclipse.dataspaceconnector.mvd;

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNode;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNodeDirectory;
import org.eclipse.dataspaceconnector.registration.client.api.RegistryApi;
import org.eclipse.dataspaceconnector.registration.client.models.Participant;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Federated cache directory using Registration Service as backend.
 */
public class RegistrationServiceNodeDirectory implements FederatedCacheNodeDirectory {

    private final RegistryApi apiClient;

    /**
     * Constructs {@link RegistrationServiceNodeDirectory}
     *
     * @param apiClient RegistrationService API client.
     */
    public RegistrationServiceNodeDirectory(RegistryApi apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public List<FederatedCacheNode> getAll() {
        return apiClient.listParticipants().stream().map(this::toFederatedCacheNode).collect(Collectors.toList());
    }

    private FederatedCacheNode toFederatedCacheNode(Participant participant) {
        return new FederatedCacheNode(participant.getName(), participant.getUrl(), participant.getSupportedProtocols());
    }

    @Override
    public void insert(FederatedCacheNode federatedCacheNode) {
        throw new UnsupportedOperationException();
    }
}
