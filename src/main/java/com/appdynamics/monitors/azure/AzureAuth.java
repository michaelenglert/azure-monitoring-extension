/*
 * Copyright 2017. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.monitors.azure;

import com.appdynamics.monitors.azure.config.AuthenticationResults;
import com.appdynamics.monitors.azure.config.Globals;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class AzureAuth {
    private static final Logger logger = LoggerFactory.getLogger(AzureAuth.class);

    public static void getAzureAuth (Map<String, ?> config) {
        String keyvaultClientSecretUrl = (String) config.get(Globals.keyvaultClientSecretUrl);
        String keyvaultClientId = (String) config.get(Globals.keyvaultClientId);
        String keyvaultClientKey = (String) config.get(Globals.keyvaultClientKey);
        String clientId = (String) config.get(Globals.clientId);
        String clientKey = (String) config.get(Globals.clientKey);
        String tenantId = (String) config.get(Globals.tenantId);
        String encryptionKey = (String) config.get(Globals.encryptionKey);
        String encryptedClientKey = (String) config.get(Globals.encryptedClientKey);
        String encryptedKeyvaultClientKey = (String) config.get(Globals.encryptedKeyvaultClientKey);

        if (!Strings.isNullOrEmpty(encryptionKey) && !Strings.isNullOrEmpty(encryptedClientKey)) {
            clientKey = Utilities.getDecryptedKey(encryptedClientKey, encryptionKey);
        }

        if (!Strings.isNullOrEmpty(encryptionKey) && !Strings.isNullOrEmpty(encryptedKeyvaultClientKey)) {
            keyvaultClientKey = Utilities.getDecryptedKey(encryptedKeyvaultClientKey, encryptionKey);
        }

        if (!Strings.isNullOrEmpty(keyvaultClientId) && !Strings.isNullOrEmpty(keyvaultClientKey) && !Strings.isNullOrEmpty(keyvaultClientSecretUrl)){
            URL keyvaultUrl = Utilities.getUrl(keyvaultClientSecretUrl + "?" + Globals.azureApiVersion +
                                "=" + config.get("keyvault-api-version"));
            AuthenticationResults.azureKeyVaultAuth = getAuthenticationResult(Globals.azureKeyvaultEndpoint,keyvaultClientId,keyvaultClientKey, tenantId);
            JsonNode keyVaultResponse = AzureRestOperation.doGet(AuthenticationResults.azureKeyVaultAuth, keyvaultUrl);
            assert keyVaultResponse != null;
            clientKey = keyVaultResponse.get("value").textValue();
        }
        AuthenticationResults.azureMonitorAuth = getAuthenticationResult(Globals.azureEndpoint + "/", clientId, clientKey, tenantId);
    }

    private static AuthenticationResult getAuthenticationResult(String endpoint, String Id, String Key, String tenantId){
        ExecutorService service = Executors.newSingleThreadExecutor();
        AuthenticationResult result = null;
        String authority = Globals.azureAuthEndpoint + tenantId;

        try {
            AuthenticationContext context;
            context = new AuthenticationContext(authority, false, service);
            ClientCredential cred = new ClientCredential(Id, Key);
            Future<AuthenticationResult> future = context.acquireToken(endpoint, cred, null);
            result = future.get();
        } catch (MalformedURLException e) {
            logger.error("Not a valid Azure authentication Authority {}", authority, e);
        } catch (InterruptedException e) {
            logger.error("Interrupt while getting Azure Authentication Result");
        } catch (ExecutionException e) {
            logger.error("Execution Exception while getting Azure Authentication Result");
        }
        service.shutdown();
        return result;
    }
}
