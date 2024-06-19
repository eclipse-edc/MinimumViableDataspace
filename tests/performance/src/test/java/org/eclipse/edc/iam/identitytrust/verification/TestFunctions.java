package org.eclipse.edc.iam.identitytrust.verification;

import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.identitytrust.model.VerifiableCredentialContainer;
import org.eclipse.edc.junit.testfixtures.TestUtils;

public class TestFunctions {
    public static VerifiableCredentialContainer createSignedCredential() {
        var cred = TestUtils.getResourceFileContentAsString("verifiable-credential-signed.json");
        return new VerifiableCredentialContainer(cred, CredentialFormat.JSON_LD, null);
    }
}
