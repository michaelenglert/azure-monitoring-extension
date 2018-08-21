/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.monitors.azure.utils;

import com.microsoft.azure.management.Azure;

public class Constants {
    public static final String DEFAULT_METRIC_PREFIX = "Custom Metrics|AzureMonitor|";
    public static final String METRIC_SEPARATOR  = "|";
    public static final String TEST_CONFIG_FILE = "src/test/resources/conf/test-config.yml";
	public static final String AZURE_MANAGEMENT_URL = "https://management.azure.com";
	public static final String AZURE_VAULT_URL = "https://vault.azure.net";
	public static final String AZURE_MSI_TOKEN_ENDPOINT = "http://169.254.169.254/metadata/identity/oauth2/token";
	public static final int AZURE_METRICS_CHUNK_SIZE = 20;
    public static Azure.Authenticated azureMonitorAuth = null;
	public static String accessToken;
}
