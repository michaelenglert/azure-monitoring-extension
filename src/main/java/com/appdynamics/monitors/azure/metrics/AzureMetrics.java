/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.monitors.azure.metrics;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.collections4.ListUtils;
import org.slf4j.LoggerFactory;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.monitors.azure.utils.Constants;
import com.appdynamics.monitors.azure.utils.Resource;
import com.appdynamics.monitors.azure.utils.MetricDefinition;
import com.appdynamics.monitors.azure.utils.AzureAPIWrapper;
import com.google.common.collect.Lists;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.fasterxml.jackson.databind.JsonNode;

@SuppressWarnings("unchecked")
public class AzureMetrics implements Runnable {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AzureMetrics.class);
    private final Map<String, ?> resourceFilter;
    private final String currentResourceGroupFilter;
    private final String currentResourceFilter;
    private final String currentResourceTypeFilter;
    private final Resource resource;
    private final String subscriptionName;
    private final CountDownLatch countDownLatch;
    private final MetricWriteHelper metricWriteHelper;
    private final String metricPrefix;
    private final List<Metric> finalMetricList = Lists.newArrayList();
    private AzureAPIWrapper azure;

    public AzureMetrics(AzureAPIWrapper azure,
                        Map<String, ?> resourceFilter,
                        String currentResourceGroupFilter,
                        String currentResourceFilter,
                        String currentResourceTypeFilter,
                        Resource resource,
                        Map<String, ?> subscription,
                        CountDownLatch countDownLatch,
                        MetricWriteHelper metricWriteHelper,
                        String metricPrefix) {
        this.azure = azure;
        this.resourceFilter = resourceFilter;
        this.currentResourceGroupFilter = currentResourceGroupFilter;
        this.currentResourceFilter = currentResourceFilter;
        this.currentResourceTypeFilter = currentResourceTypeFilter;
        this.resource = resource;
        this.countDownLatch = countDownLatch;
        this.metricWriteHelper = metricWriteHelper;
        this.metricPrefix = metricPrefix;
        if (subscription.containsKey("subscriptionName")){
            subscriptionName = subscription.get("subscriptionName").toString();
        }
        else {
            subscriptionName = subscription.get("subscriptionId").toString();
        }
    }

    @Override
    public void run() {
        try {
            runTask();
        }
        catch(Exception e){
            logger.error(e.getMessage());
        }
        finally {
            countDownLatch.countDown();
        }
    }

    private void runTask(){
//        if (logger.isDebugEnabled()) {
//            logger.debug("Resource name ({}): {} {}", resource.name().matches(currentResourceFilter), resource.name(), currentResourceFilter);
//            logger.debug("Resource type ({}): {} {}", resource.type().matches(currentResourceTypeFilter), resource.type(), currentResourceTypeFilter);
//            logger.debug("Resource group ({}): {} {}", resource.resourceGroupName().matches("(?i:" + currentResourceGroupFilter + ")"), resource.resourceGroupName(), currentResourceGroupFilter);
//        }
        if (resource.getName().matches(currentResourceFilter) &&
                resource.getType().matches(currentResourceTypeFilter) &&
                resource.getResourceGroupName().matches("(?i:" + currentResourceGroupFilter + ")")) {
            if (logger.isDebugEnabled()) {
                logger.debug("Working on Resource {} of Type {} in Group {} because of Resource Filter {} of Type Filter {} in Group Filter {}",
                        resource.getName(),
                        resource.getResourceType(),
                        resource.getResourceGroupName(),
                        currentResourceTypeFilter,
                        currentResourceTypeFilter,
                        currentResourceGroupFilter);
            }
            List<MetricDefinition> metricDefinitions = azure.getMetricDefinitions(resource.getId());
            try {
                generateMetrics(metricDefinitions);
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
//            if (logger.isDebugEnabled()) {
//                logger.debug("Skipping Resource {} of Type {} in Group {} because of Resource Filter {} of Type Filter {} in Group Filter {}",
//                        resource.name(),
//                        resource.resourceType(),
//                        resource.resourceGroupName(),
//                        currentResourceTypeFilter,
//                        currentResourceTypeFilter,
//                        currentResourceGroupFilter);
//            }
        }
        if (finalMetricList.isEmpty()) {
//            if (logger.isDebugEnabled()) {
//                logger.debug("Metric List is empty");
//            }
        } else {
            metricWriteHelper.transformAndPrintMetrics(finalMetricList);
        }
//        if (logger.isDebugEnabled()) {
//            logger.debug("azureMetricDefinitionsCallCount {}: {}", resource.id(), azureMetricDefinitionsCallCount);
//            logger.debug("azureMetricsCallCount: " + azureMetricsCallCount);
//        }
    }

    private void generateMetrics(List<MetricDefinition> metricDefinitions) throws MalformedURLException, UnsupportedEncodingException {
        List<Map<String, ?>> metricConfigs = (List<Map<String, ?>>) resourceFilter.get("metrics");

        List<MetricDefinition> filteredMetrics = Lists.newArrayList();
        List<String> filteredMetricsNames = Lists.newArrayList();
        List<Map<String, ?>> filteredMetricsConfig = Lists.newArrayList();

        for (Map<String, ?> metricConfig : metricConfigs) {
            for (MetricDefinition metricDefinition : metricDefinitions) {
                String currentMetricConfig = metricConfig.get("metric").toString();
//                if (logger.isDebugEnabled()) {
//                    logger.debug("resourceMetric name ({})", resourceMetric.name().value());
//                    logger.debug("currentMetricConfig ({})", currentMetricConfig);
//                    logger.debug("match ({})", resourceMetric.name().value().matches(currentMetricConfig), resourceMetric.name().value(), currentMetricConfig);
//                }
                if (metricDefinition.getName().matches(currentMetricConfig)) {
                    Object filterObject = metricConfig.get("filter");
                    String filter = filterObject == null ? null : filterObject.toString();
                    boolean hasQualifiers = hasQualifiers(metricConfig);
                    if (( filter != null && !filter.isEmpty() ) || hasQualifiers ) {
                        JsonNode apiResponse = azure.getMetrics(metricDefinition, URLEncoder.encode(metricDefinition.getName(), "UTF-8"), filter);
                        try {
                            addMetric(metricDefinition, apiResponse, metricConfig);
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    } else {
                        filteredMetrics.add(metricDefinition);
                        filteredMetricsNames.add(URLEncoder.encode(metricDefinition.getName(), "UTF-8"));
                        filteredMetricsConfig.add(metricConfig);
                    }
                } else {
//                    if (logger.isDebugEnabled()) {
//                        logger.debug("Not Reporting Metric {} for Resource {} as it is filtered by {}",
//                                resourceMetric.name().value(),
//                                resource.name(),
//                                currentMetricConfig);
//                    }
                }
            }
        }
//        logger.debug("filteredMetrics size ({})", filteredMetrics.size());
        consumeAndImportAzureMetrics(filteredMetrics, filteredMetricsNames, filteredMetricsConfig);
    }

    public boolean hasQualifiers(Map<String, ?> metricConfig) {
        Object aggregatorObject = metricConfig.get("aggregator");
        String aggregator = aggregatorObject == null ? "" : aggregatorObject.toString();
        Object timeRollUpObject = metricConfig.get("timeRollUp");
        String timeRollUp = timeRollUpObject == null ? "" : timeRollUpObject.toString();
        Object clusterRollUpObject = metricConfig.get("clusterRollUp");
        String clusterRollUp = clusterRollUpObject == null ? "" : clusterRollUpObject.toString();
        boolean hasQualifiers = !aggregator.isEmpty() || !timeRollUp.isEmpty() || !clusterRollUp.isEmpty();
        return hasQualifiers;
    }

    private void consumeAndImportAzureMetrics(List<MetricDefinition> filteredMetrics, List<String> filteredMetricsNames, List<Map<String, ?>> filteredMetricsConfig) throws MalformedURLException, UnsupportedEncodingException {
        if (!filteredMetrics.isEmpty()) {
            List<List<MetricDefinition>> filteredMetricsChunks = ListUtils.partition(filteredMetrics, Constants.AZURE_METRICS_CHUNK_SIZE);
            List<List<String>> filteredMetricsNamesChunks = ListUtils.partition(filteredMetricsNames, Constants.AZURE_METRICS_CHUNK_SIZE);
            List<List<Map<String, ?>>> filteredMetricsConfigChunks = ListUtils.partition(filteredMetricsConfig, Constants.AZURE_METRICS_CHUNK_SIZE);
            Iterator<List<MetricDefinition>> filteredMetricsIterator = filteredMetricsChunks.iterator();
            Iterator<List<String>> filteredMetricsNamesIterator = filteredMetricsNamesChunks.iterator();
            Iterator<List<Map<String, ?>>> filteredMetricsConfigIterator = filteredMetricsConfigChunks.iterator();

            while (filteredMetricsIterator.hasNext() && filteredMetricsNamesIterator.hasNext() && filteredMetricsConfigIterator.hasNext()) {
                List<MetricDefinition> filteredMetricsChunk = filteredMetricsIterator.next();
                List<String> filteredMetricsNamesChunk = filteredMetricsNamesIterator.next();
                List<Map<String, ?>> filteredMetricsConfigChunk = filteredMetricsConfigIterator.next();
                JsonNode apiResponse = azure.getMetrics(filteredMetrics.get(0), StringUtils.join(filteredMetricsNamesChunk, ','));
                if (apiResponse != null) {
                    addMetrics(filteredMetricsChunk, filteredMetricsConfigChunk, apiResponse);
                }
            }
        }
    }

    private void addMetrics(List<MetricDefinition> filteredMetricsChunk, List<Map<String, ?>> filteredMetricsConfigChunk, JsonNode responseMetrics) {

        JsonNode responseMetricsValues = responseMetrics.get("value");
        Iterator<JsonNode> responseMetricsIterator = responseMetricsValues.elements();
        Iterator<MetricDefinition> filteredMetricsIterator = filteredMetricsChunk.iterator();
        Iterator<Map<String, ?>> filteredMetricsConfigIterator = filteredMetricsConfigChunk.iterator();
        while (responseMetricsIterator.hasNext() && filteredMetricsConfigIterator.hasNext() && filteredMetricsIterator.hasNext()) {
            MetricDefinition resourceMetric = filteredMetricsIterator.next();
            Map<String, ?> resourceMetricConfig = filteredMetricsConfigIterator.next();
            JsonNode responseMetric = responseMetricsIterator.next();
//            if (logger.isDebugEnabled()) {
//                logger.debug("Response metric: {}", responseMetric.toString());
//            }
            try {
                addMetric(resourceMetric, responseMetric, resourceMetricConfig);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void addMetric(MetricDefinition resourceMetric, JsonNode responseMetric, Map<String, ?> metricConfig) throws Exception {
        if (responseMetric == null) return;
        String azureMetricValue = null;
        String metricPath = metricPrefix +
                Constants.METRIC_SEPARATOR +
                subscriptionName +
                Constants.METRIC_SEPARATOR +
                resource.getResourceGroupName() +
                Constants.METRIC_SEPARATOR +
                resource.getResourceType() +
                Constants.METRIC_SEPARATOR +
                resource.getName() +
                Constants.METRIC_SEPARATOR;
//        if (logger.isDebugEnabled()) {
//            logger.debug("Metric name / aggregation / path: {} / {} / {}", metricName, metricAggregation, metricPath);
//        }

        JsonNode timeseries = responseMetric.findPath("timeseries");
//        logger.debug("timeseries.size():" + timeseries.size());
        if (timeseries.size() > 1) {
            throw new Exception("Multiple timeseries not supported");
        }
        String azureMetricName = resourceMetric.getName();
        String azureMetricAggregation = resourceMetric.getPrimaryAggregationType().toString();
        String appdAggregationType = null;
        String appdTimeRollUpType = null;
        String appdClusterRollUpType = null;
        JsonNode data = timeseries.findPath("data");
        switch (azureMetricAggregation) {
            case "Average":
                azureMetricValue = (data.path(0).path("average").isMissingNode()) ? null : data.get(0).get("average").toString();
                appdAggregationType = MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE;
                appdTimeRollUpType = MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE;
                appdClusterRollUpType = MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL;
                break;
            case "Count":
                azureMetricValue = (data.path(0).path("count").isMissingNode()) ? null : data.get(0).get("count").toString();
                appdAggregationType = MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE;
                appdTimeRollUpType = MetricWriter.METRIC_TIME_ROLLUP_TYPE_SUM;
                appdClusterRollUpType = MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE;
                break;
            case "Total":
                azureMetricValue = (data.path(0).path("total").isMissingNode()) ? null : data.get(0).get("total").toString();
                appdAggregationType = MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE;
                appdTimeRollUpType = MetricWriter.METRIC_TIME_ROLLUP_TYPE_SUM;
                appdClusterRollUpType = MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE;
                break;
            case "Maximum":
                azureMetricValue = (data.path(0).path("maximum").isMissingNode()) ? null : data.get(0).get("maximum").toString();
                appdAggregationType = MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE;
                appdTimeRollUpType = MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE;
                appdClusterRollUpType = MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL;
                break;
            case "Mininum":
                azureMetricValue = (data.path(0).path("minimum").isMissingNode()) ? null : data.get(0).get("minimum").toString();
                appdAggregationType = MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE;
                appdTimeRollUpType = MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE;
                appdClusterRollUpType = MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL;
                break;
            default:
                if (logger.isDebugEnabled()) {
                    logger.info("Not Reporting Metric {} for Resource {} as the aggregation type is not supported",
                            azureMetricName,
                            resource.getName());
                }
                break;
        }
        if (azureMetricValue == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Ignoring Metric {} for Resource {} as it is null or empty",
                        azureMetricName,
                        resource.getName(),
                        azureMetricValue);
            }
        } else {
            Object subpathObject = null;
            Object aliasObject = null;
            Object aggregatorObject = null;
            Object timeRollUpObject = null;
            Object clusterRollUpObject = null;
            if (metricConfig != null) {
                aliasObject = metricConfig.get("alias");
                subpathObject = metricConfig.get("subpath");
                aggregatorObject = metricConfig.get("aggregator");
                timeRollUpObject = metricConfig.get("timeRollUp");
                clusterRollUpObject = metricConfig.get("clusterRollUp");
            }
            azureMetricName = aliasObject == null ? azureMetricName : aliasObject.toString();
            String subpath = "";
            subpath = subpathObject == null ? "" : subpathObject.toString() + "|";
            appdAggregationType = aggregatorObject == null ? appdAggregationType : MetricWriter.class.getDeclaredField(aggregatorObject.toString()).get(null).toString();
            appdTimeRollUpType = timeRollUpObject == null ? appdTimeRollUpType : MetricWriter.class.getDeclaredField(timeRollUpObject.toString()).get(null).toString();
            appdClusterRollUpType = clusterRollUpObject == null ? appdClusterRollUpType : MetricWriter.class.getDeclaredField(clusterRollUpObject.toString()).get(null).toString();
            Metric metric = new Metric(azureMetricName,
                    azureMetricValue,
                    metricPath + subpath + azureMetricName,
                    appdAggregationType,
                    appdTimeRollUpType,
                    appdClusterRollUpType);
            if (logger.isDebugEnabled()) {
                logger.debug("Reporting Metric {} for Resource {} with value {}",
                        azureMetricName,
                        resource.getName(),
                        azureMetricValue);
            }
            finalMetricList.add(metric);
        }
    }
}
