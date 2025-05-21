
package framework;

import com.microsoft.aad.msal4j.*;
import java.util.*;
import java.util.concurrent.*;

public class AzureTokenProvider {
    private final String clientId;
    private final String clientSecret;
    private final String authority;
    private final String scope;

    public AzureTokenProvider(String clientId, String clientSecret, String authority, String scope) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.authority = authority;
        this.scope = scope;
    }

    public String getAccessToken() throws Exception {
        ConfidentialClientApplication app = ConfidentialClientApplication.builder(
                clientId, ClientCredentialFactory.createFromSecret(clientSecret))
            .authority(authority)
            .build();

        ClientCredentialParameters parameters = ClientCredentialParameters.builder(
                Collections.singleton(scope))
            .build();

        IAuthenticationResult result = app.acquireToken(parameters).get();
        return result.accessToken();
    }
}
