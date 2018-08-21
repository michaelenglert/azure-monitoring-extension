/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.monitors.azure;

import com.appdynamics.monitors.azure.utils.Constants;
import com.appdynamics.monitors.azure.utils.Utilities;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.credentials.MSICredentials;
import com.microsoft.azure.management.Azure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class AzureAuth {
    private static final Logger logger = LoggerFactory.getLogger(AzureAuth.class);

    static void getAzureAuth(Map<String, ?> subscription) {
        Boolean useMSI = subscription.get("useMSI") == null ? false : Boolean.valueOf(subscription.get("useMSI").toString());
        String keyvaultClientSecretUrl = (String) subscription.get("keyvaultClientSecretUrl");
        String keyvaultClientId = (String) subscription.get("keyvaultClientId");
        String keyvaultClientKey = (String) subscription.get("keyvaultClientKey");
        String clientId = (String) subscription.get("clientId");
        String clientKey = (String) subscription.get("clientKey");
        String tenantId = (String) subscription.get("tenantId");
        String encryptionKey = (String) subscription.get("encryption-key");
        String encryptedClientKey = (String) subscription.get("encryptedClientKey");
        String encryptedKeyvaultClientKey = (String) subscription.get("encryptedKeyvaultClientKey");

        if (useMSI) {
            MSICredentials credentials = new MSICredentials(AzureEnvironment.AZURE);
            if (logger.isDebugEnabled()) {
                Constants.azureMonitorAuth = Azure.configure()
                        .withLogLevel(com.microsoft.rest.LogLevel.BASIC)
                        .authenticate(credentials);
            } else {
                Constants.azureMonitorAuth = Azure.authenticate(credentials);
            }

            Constants.accessToken = getMSIAccessToken(subscription);
        } else {

            if (!Strings.isNullOrEmpty(encryptionKey) && !Strings.isNullOrEmpty(encryptedClientKey)) {
                clientKey = Utilities.getDecryptedKey(encryptedClientKey, encryptionKey);
            }

            if (!Strings.isNullOrEmpty(encryptionKey) && !Strings.isNullOrEmpty(encryptedKeyvaultClientKey)) {
                keyvaultClientKey = Utilities.getDecryptedKey(encryptedKeyvaultClientKey, encryptionKey);
            }

            if (!Strings.isNullOrEmpty(keyvaultClientId) && !Strings.isNullOrEmpty(keyvaultClientKey) && !Strings.isNullOrEmpty(keyvaultClientSecretUrl)){
                URL keyvaultUrl = Utilities.getUrl(keyvaultClientSecretUrl + "?" + "api-version" +
                                    "=" + subscription.get("keyvault-api-version"));
                AuthenticationResult azureKeyVaultAuth = getAuthenticationResult(keyvaultClientId, keyvaultClientKey, tenantId, Constants.AZURE_VAULT_URL);
                Constants.accessToken = azureKeyVaultAuth.getAccessToken();
                logger.debug("Bearer {}", azureKeyVaultAuth.getAccessToken());
                JsonNode keyVaultResponse = AzureRestOperation.doGet(keyvaultUrl);
                assert keyVaultResponse != null;
                clientKey = keyVaultResponse.get("value").textValue();
            }

            ApplicationTokenCredentials applicationTokenCredentials = new ApplicationTokenCredentials(
                    clientId,
                    tenantId,
                    clientKey,
                    AzureEnvironment.AZURE);
            if (logger.isDebugEnabled()) {
                Constants.azureMonitorAuth = Azure.configure()
                        .withLogLevel(com.microsoft.rest.LogLevel.BASIC)
                        .authenticate(applicationTokenCredentials);
            } else {
                Constants.azureMonitorAuth = Azure.authenticate(applicationTokenCredentials);
            }
            AuthenticationResult azureAuthResult = getAuthenticationResult(clientId, clientKey, tenantId, Constants.AZURE_MANAGEMENT_URL);
            Constants.accessToken = azureAuthResult.getAccessToken();
            logger.debug("Bearer {}", azureAuthResult.getAccessToken());
        }
    }

    private static String getMSIAccessToken(Map<String, ?> subscription) {
        String resource = null;
        try {
            resource = URLEncoder.encode("https://management.azure.com/", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        URL url = Utilities.getUrl(Constants.AZURE_MSI_TOKEN_ENDPOINT
                + "?api-version=" + subscription.get("msi-token-api-version")
                + "&resource=" + resource);
        Map<String, String> headers = new HashMap<>();
        headers.put("Metadata", "true");
        JsonNode keyVaultResponse = AzureRestOperation.doGet(url, headers);

        return keyVaultResponse.get("access_token").textValue();
    }

    private static AuthenticationResult getAuthenticationResult(String Id, String Key, String tenantId, String resourceUrl){
        ExecutorService service = Executors.newSingleThreadExecutor();
        AuthenticationResult result = null;
        String authority = "https://login.microsoftonline.com/" + tenantId;

        try {
            AuthenticationContext context = new AuthenticationContext(authority, false, service);
            ClientCredential cred = new ClientCredential(Id, Key);
            Future<AuthenticationResult> future = context.acquireToken(resourceUrl, cred, null);
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
