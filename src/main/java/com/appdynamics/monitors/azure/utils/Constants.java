/*
 * Copyright 2017. AppDynamics LLC and its affiliates.
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
    public static Azure.Authenticated azureMonitorAuth = null;
}
