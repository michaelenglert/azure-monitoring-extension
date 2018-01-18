/*
 * Copyright 2017. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.monitors.azure;

import com.appdynamics.monitors.azure.config.Globals;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class AzureAuth {
    private static final Logger logger = LoggerFactory.getLogger(AzureAuth.class);

    public static AuthenticationResult getAzureAuth (String clientId, String clientKey,String tenantId) {
        ExecutorService service = Executors.newSingleThreadExecutor();
        AuthenticationContext context;
        AuthenticationResult result = null;
        String authority = Globals.azureAuthEndpoint + tenantId;
        try {
            context = new AuthenticationContext(authority, false, service);
            ClientCredential cred = new ClientCredential(clientId, clientKey);
            Future<AuthenticationResult> future = context.acquireToken(Globals.azureEndpoint + "/", cred, null);
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
