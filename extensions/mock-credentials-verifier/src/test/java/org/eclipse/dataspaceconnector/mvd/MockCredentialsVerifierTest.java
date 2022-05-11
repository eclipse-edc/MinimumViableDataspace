package org.eclipse.dataspaceconnector.mvd;

import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class MockCredentialsVerifierTest {
    MockCredentialsVerifier verifier = new MockCredentialsVerifier(new ConsoleMonitor());
    PublicKeyWrapper wrapper = mock(PublicKeyWrapper.class);

    @Test
    void verifyCredentials() {
        Result<Map<String, String>> actual = verifier.verifyCredentials("http://dummy.site/foo?region=us&tier=GOLD", wrapper);
        assertThat(actual.succeeded()).isTrue();
        assertThat(actual.getContent())
                .isEqualTo(Map.of("region", "us", "tier", "GOLD"));
    }

    @Test
    void verifyCredentials_failsOnMalformedUrl() {
        assertThatThrownBy(() -> verifier.verifyCredentials("malformed_url", wrapper))
                .isInstanceOf(EdcException.class);
    }
}