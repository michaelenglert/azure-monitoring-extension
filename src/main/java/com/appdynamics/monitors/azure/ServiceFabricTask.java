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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.Map;

class ServiceFabricTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(AzureMonitorTask.class);
    private final MonitorConfiguration configuration;
    private final JsonNode node;
    private final String metric;

    ServiceFabricTask(MonitorConfiguration configuration, JsonNode node, String metric) {
        this.configuration = configuration;
        this.node = node;
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
        if (logger.isDebugEnabled()) {logger.debug("JSON Node: " + Utilities.prettifyJson(node));}
        URL url = new URL(node.get("properties").get("managementEndpoint").asText() + Globals.serviceFabricGetClusterHealthChunk +
                "?" + Globals.azureApiVersion + "=" + configuration.getConfigYml().get(Globals.serviceFabricApiVersion));
        if (logger.isDebugEnabled()) {logger.debug("Get Metrics REST API Request: " + url.toString());}
        if (url.toString().matches("https://.*")){
            logger.info("Skipping Service Fabric Cluster {} because the Authentication Method is currently not supported",
                    node.get("properties").get("managementEndpoint").asText());
        }
        else {
            extractMetrics(AzureRestOperation.doPost(url, configuration.getConfigYml().get(Globals.serviceFabricBody).toString()));
        }
    }

    private void extractMetrics(JsonNode json){
        if (logger.isDebugEnabled()) {logger.debug("Get Metrics Response JSON: " + Utilities.prettifyJson(json));}
        List healtStateList = (List) configuration.getConfigYml().get(Globals.serviceFabricHealthStates);
        Map healtStates = (Map) healtStateList.get(0);
        String metricName = configuration.getMetricPrefix() + "|" + metric + "|";
        MetricPrinter metricPrinter = new MetricPrinter(configuration.getMetricWriter());
        metricPrinter.reportMetric(metricName +
                "ClusterHealth",
                BigDecimal.valueOf((Integer) healtStates.get(json.get("HealthState").asText())));

        JsonNode nodes = json.get("NodeHealthStateChunks").get("Items");
        for (JsonNode node:nodes){
            metricPrinter.reportMetric(metricName +
                    "NodeHealth" +
                    "|" + node.get("NodeName").asText(),
                    BigDecimal.valueOf((Integer) healtStates.get(node.get("HealthState").asText())));
        }

        JsonNode apps = json.get("ApplicationHealthStateChunks").get("Items");
        for (JsonNode app:apps){
            if (app.get("ApplicationTypeName").asText().isEmpty()){
                metricPrinter.reportMetric(metricName +
                        "ApplicationHealth" +
                        "|" + "System",
                        BigDecimal.valueOf((Integer) healtStates.get(app.get("HealthState").asText())));
            }
            else {
                metricPrinter.reportMetric(metricName +
                        "ApplicationHealth" +
                        "|" + app.get("ApplicationTypeName").asText(),
                        BigDecimal.valueOf((Integer) healtStates.get(app.get("HealthState").asText())));
            }
        }
    }
}