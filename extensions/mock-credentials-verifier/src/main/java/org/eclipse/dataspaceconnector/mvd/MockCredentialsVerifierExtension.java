package org.eclipse.dataspaceconnector.mvd;

import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.iam.did.spi.document.DidConstants.DID_URL_SETTING;

/**
 * Extension to set up the {@link MockCredentialsVerifier} service to generate stub claims.
 */
@Provides(CredentialsVerifier.class)
public class MockCredentialsVerifierExtension implements ServiceExtension {

    @Override
    public void initialize(ServiceExtensionContext context) {
        var credentialsVerifier = new MockCredentialsVerifier(context.getMonitor());
        context.registerService(CredentialsVerifier.class, credentialsVerifier);
    }
}
