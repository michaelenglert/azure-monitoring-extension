/*
 * Copyright 2017. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.monitors.azure.config;

import com.microsoft.aad.adal4j.AuthenticationResult;

public class AuthenticationResults {
    public static AuthenticationResult azureMonitorAuth = null;
    public static AuthenticationResult azureKeyVaultAuth = null;
}
