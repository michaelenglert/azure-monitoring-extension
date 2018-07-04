/*
 * Copyright 2017. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.monitors.azure;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.monitors.azure.utils.Constants;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class ServiceFabricTask implements AMonitorTaskRunnable {
    private static final Logger logger = LoggerFactory.getLogger(ServiceFabricTask.class);
    private final MonitorContextConfiguration configuration;
    private final MetricWriteHelper metricWriteHelper;
    private final Map<String, ?> serviceFabric;
    private final String managementEndpoint;
    private final String metricName;
    private List<Metric> finalMetricList;

    private ServiceFabricTask(MonitorContextConfiguration configuration, MetricWriteHelper metricWriteHelper, Map<String, ?> serviceFabric, String managementEndpoint, String metricName) {
        this.configuration = configuration;
        this.metricWriteHelper = metricWriteHelper;
        this.serviceFabric = serviceFabric;
        this.managementEndpoint = managementEndpoint;
        this.metricName = metricName;
    }

    @Override
    public void onTaskComplete() {
        logger.info("Task Complete");
    }

    @Override
    public void run() {
        try {
            runTask();
        } catch (Exception e) {
            metricWriteHelper.registerError(e.getMessage(), e);
            logger.error("Error while running the task", e);
        }
    }

    private void runTask() throws IOException {
        String serviceFabricBody = serviceFabric.get("serviceFabricBody").toString();
        String serviceFabricCert = serviceFabric.get("serviceFabricCert").toString();
        String serviceFabricPassphrase = serviceFabric.get("serviceFabricPassphrase").toString();
        finalMetricList = Lists.newArrayList();

        URL url = new URL(managementEndpoint + "/$/GetClusterHealthChunk" +
                "?api-version=" + serviceFabric.get("serviceFabricApiVersion"));
        if (logger.isDebugEnabled()) {logger.debug("Get Metrics REST API Request: " + url.toString());}
        if (url.toString().matches("https://.*") && !serviceFabricCert.isEmpty()){
            extractMetrics(AzureRestOperation.doSecurePost(url, serviceFabricBody, serviceFabricCert, serviceFabricPassphrase));
        }
        else {
            extractMetrics(Objects.requireNonNull(AzureRestOperation.doPost(url, serviceFabricBody)));
        }
    }

    private void extractMetrics(JsonNode json){
        List healtStateList = (List) serviceFabric.get("serviceFabricHealthStates");
        Metric metric;
        Map healtStates = (Map) healtStateList.get(0);
        String metricPath = configuration.getMetricPrefix() + Constants.METRIC_SEPARATOR + metricName + Constants.METRIC_SEPARATOR;
        metric = new Metric("ClusterHealth", healtStates.get(json.get("HealthState").asText()).toString(), metricPath);
        finalMetricList.add(metric);
        JsonNode nodes = json.get("NodeHealthStateChunks").get("Items");
        for (JsonNode node:nodes){
            metric = new Metric(node.get("NodeName").asText(), healtStates.get(node.get("HealthState").asText()).toString(), metricPath +
                    "NodeHealth" +
                    Constants.METRIC_SEPARATOR);
            finalMetricList.add(metric);
        }

        JsonNode apps = json.get("ApplicationHealthStateChunks").get("Items");
        for (JsonNode app:apps){
            if (app.get("ApplicationTypeName").asText().isEmpty()){
                metric = new Metric("System", healtStates.get(app.get("HealthState").asText()).toString(), metricPath +
                        "ApplicationHealth" +
                        Constants.METRIC_SEPARATOR);
                finalMetricList.add(metric);
            }
            else {
                metric = new Metric(app.get("ApplicationTypeName").asText(), healtStates.get(app.get("HealthState").asText()).toString(), metricPath +
                        "ApplicationHealth" +
                        Constants.METRIC_SEPARATOR);
                finalMetricList.add(metric);
            }
        }
        if (finalMetricList.isEmpty()){
            if (logger.isDebugEnabled()) {
                logger.debug("Metric List is empty");
            }
        }
        else {
            metricWriteHelper.transformAndPrintMetrics(finalMetricList);
        }
    }
}