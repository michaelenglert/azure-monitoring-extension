/*
 * Copyright 2017. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.monitors.azure;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.monitors.azure.config.Globals;
import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.aad.adal4j.AuthenticationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

class AzureMonitorTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(AzureMonitorTask.class);
    private final MonitorConfiguration configuration;
    private final JsonNode node;
    private final AuthenticationResult azureAuth;
    private final String metric;

    AzureMonitorTask(MonitorConfiguration configuration, JsonNode node, AuthenticationResult azureAuth, String metric) {
        this.configuration = configuration;
        this.node = node;
        this.azureAuth = azureAuth;
        this.metric = metric;
    }

    public void run() {
        try {
            runTask();
        } catch (Exception e) {
            configuration.getMetricWriter().registerError(e.getMessage(), e);
            logger.error("Error while running the task", e);
        }
    }

    private void runTask() throws IOException {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        Calendar endTime = Calendar.getInstance();
        Calendar startTime = Calendar.getInstance();
        startTime.add(Calendar.MINUTE, Globals.timeOffset);
        SimpleDateFormat dateFormatter = new SimpleDateFormat(Globals.azureApiTimeFormat);
        dateFormatter.setTimeZone(utc);
        if (logger.isDebugEnabled()) {logger.debug("JSON Node: " + Utilities.prettifyJson(node));}
        URL url = new URL(Globals.azureEndpoint + node.get("id").asText() + Globals.azureApiMetrics +
                "?" + Globals.azureApiVersion + "=" + configuration.getConfigYml().get(Globals.azureMonitorApiVersion) +
                "&" + Globals.azureApiTimeSpan + "=" + dateFormatter.format(startTime.getTime()) + "/" + dateFormatter.format(endTime.getTime()) +
                "&" + Globals.metric + "=" + URLEncoder.encode(metric,Globals.urlEncoding));
        if (logger.isDebugEnabled()) {logger.debug("Get Metrics REST API Request: " + url.toString());}
        extractMetrics(AzureRestOperation.doGet(azureAuth,url));
    }

    private void extractMetrics(JsonNode json){
        if (logger.isDebugEnabled()) {logger.debug("Get Metrics Response JSON: " + Utilities.prettifyJson(json));}
        JsonNode jsonValue = json.get("value");
        for (JsonNode currentValueNode:jsonValue){
            String metricId = extractMetridId(currentValueNode.get("id").asText());
            String metricNameValue = currentValueNode.get("name").get("value").asText();
            String metricUnit = currentValueNode.get("unit").asText();
            String metricType = null;
            BigDecimal metricValue = null;
            if(currentValueNode.get("timeseries").has(0)){
                JsonNode jsonData = currentValueNode.get("timeseries").get(0).get("data");
                for (JsonNode currentDataNode:jsonData){
                    if (currentDataNode.has("average")){ metricType = "average"; metricValue = currentDataNode.get("average").decimalValue(); }
                    else if (currentDataNode.has("total")){ metricType = "total"; metricValue = currentDataNode.get("total").decimalValue(); }
                    else if (currentDataNode.has("last")){ metricType = "last"; metricValue = currentDataNode.get("last").decimalValue(); }
                    else if (currentDataNode.has("maximum")){ metricType = "maximum"; metricValue = currentDataNode.get("maximum").decimalValue(); }
                }
                if (metricId != null && metricNameValue != null && metricType != null && metricUnit != null && metricValue != null){
                    MetricPrinter metricPrinter = new MetricPrinter(configuration.getMetricWriter());
                    metricPrinter.reportMetric(configuration.getMetricPrefix() + metricId + metricNameValue, metricValue);
                }
            }
        }
    }

    private static String extractMetridId(String fullId){
        String metricId;
        String[] metricIdSegments = fullId.split("resourceGroups")[1].split("providers/Microsoft\\.");
        metricId = metricIdSegments[0]+metricIdSegments[1];
        metricId = metricId.replace("/","|");
        return metricId;
    }
}