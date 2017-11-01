package com.appdynamics.monitors.azure.config;

public class Globals {
    public static final String azureAuthEndpoint = "https://login.microsoftonline.com/";
    public static final String azureEndpoint = "https://management.azure.com";
    public static final String azureApiVersion = "api-version";
    public static final String azureMonitorApiVersion = "monitor-api-version";
    public static final String defaultMetricPrefix = "Custom Metrics|AzureMonitor|";
    public static final String azureApiMetrics = "/providers/microsoft.insights/metrics";
    public static final String azureApiSubscriptions = "/subscriptions/";
    public static final String azureApiResources = "/resources";
    public static final String azureApiFilter = "filter";
    public static final String azureApiTimeSpan = "timespan";
    public static final String azureApiTimeFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String filterBy = "resourceType";
    public static final String filterLogOp = " or ";
    public static final String filterComOp = " eq ";
    public static final String urlEncoding = "UTF-8";
    public static final String configFile = "config-file";
    public static final String clientId = "clientId";
    public static final String tenantId = "tenantId";
    public static final String clientKey = "clientKey";
    public static final String subscriptionId = "subscriptionId";
    public static final String encryptionKey = "encryption-key";
    public static final String encryptedClientKey = "encryptedClientKey";
    public static final String passwordEncrypted = "password-encrypted";
    public static final int timeOffset = -2;
}
