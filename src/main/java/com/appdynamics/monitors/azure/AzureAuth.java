package com.appdynamics.monitors.azure;

import com.appdynamics.monitors.azure.config.Globals;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class AzureAuth {
    public static AuthenticationResult getAzureAuth (String clientId, String clientKey,String tenantId) throws MalformedURLException, ExecutionException, InterruptedException {
        ExecutorService service = Executors.newFixedThreadPool(1);
        AuthenticationContext context;
        AuthenticationResult result;
        context = new AuthenticationContext(Globals.azureAuthEndpoint + tenantId, false, service);
        ClientCredential cred = new ClientCredential(clientId, clientKey);
        Future<AuthenticationResult> future = context.acquireToken(Globals.azureEndpoint + "/", cred, null);
        result = future.get();

        return result;
    }
}
