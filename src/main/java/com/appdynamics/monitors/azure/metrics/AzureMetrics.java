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
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.collections4.ListUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.LoggerFactory;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.monitors.azure.AzureRestOperation;
import com.appdynamics.monitors.azure.utils.Constants;
import com.google.common.collect.Lists;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.monitor.MetricCollection;
import com.microsoft.azure.management.monitor.MetricDefinition;
import com.microsoft.azure.management.resources.GenericResource;
import com.fasterxml.jackson.databind.JsonNode;

@SuppressWarnings("unchecked")
public class AzureMetrics implements AMonitorTaskRunnable {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AzureMetrics.class);

    public final AtomicInteger azureMetricsCallCount = new AtomicInteger(0);
    public final AtomicInteger azureMetricDefinitionsCallCount = new AtomicInteger(0);
    private final Map<String, ?> resourceFilter;
    private final String currentResourceGroupFilter;
    private final String currentResourceFilter;
    private final String currentResourceTypeFilter;
    private final GenericResource resource;
    private final Map<String, ?> subscription;
    private final String subscriptionName;
    private final CountDownLatch countDownLatch;
    private final MetricWriteHelper metricWriteHelper;
    private final Azure azure;
    private final String metricPrefix;
    private final List<Metric> finalMetricList = Lists.newArrayList();
    private final DateTime recordDateTime = DateTime.now(DateTimeZone.UTC);

    public AzureMetrics(Map<String, ?> resourceFilter,
                        String currentResourceGroupFilter,
                        String currentResourceFilter,
                        String currentResourceTypeFilter,
                        GenericResource resource,
                        Map<String, ?> subscription,
                        CountDownLatch countDownLatch,
                        MetricWriteHelper metricWriteHelper,
                        Azure azure,
                        String metricPrefix) {
        this.resourceFilter = resourceFilter;
        this.currentResourceGroupFilter = currentResourceGroupFilter;
        this.currentResourceFilter = currentResourceFilter;
        this.currentResourceTypeFilter = currentResourceTypeFilter;
        this.resource = resource;
        this.subscription = subscription;
        this.countDownLatch = countDownLatch;
        this.metricWriteHelper = metricWriteHelper;
        this.azure = azure;
        this.metricPrefix = metricPrefix;
        if (subscription.containsKey("subscriptionName")){
            subscriptionName = subscription.get("subscriptionName").toString();
        }
        else {
            subscriptionName = subscription.get("subscriptionId").toString();
        }
    }

    @Override
    public void onTaskComplete() {
        logger.info("Task Complete");
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
        if (logger.isDebugEnabled()) {
            logger.debug("Resource name ({}): {} {}", resource.name().matches(currentResourceFilter), resource.name(), currentResourceFilter);
            logger.debug("Resource type ({}): {} {}", resource.type().matches(currentResourceTypeFilter), resource.type(), currentResourceTypeFilter);
            logger.debug("Resource group ({}): {} {}", resource.resourceGroupName().matches("(?i:" + currentResourceGroupFilter + ")"), resource.resourceGroupName(), currentResourceGroupFilter);
        }
        if (resource.name().matches(currentResourceFilter) &&
                resource.type().matches(currentResourceTypeFilter) &&
                resource.resourceGroupName().matches("(?i:" + currentResourceGroupFilter + ")")) {
            if (logger.isDebugEnabled()) {
                logger.debug("Working on Resource {} of Type {} in Group {} because of Resource Filter {} of Type Filter {} in Group Filter {}",
                        resource.name(),
                        resource.resourceType(),
                        resource.resourceGroupName(),
                        currentResourceTypeFilter,
                        currentResourceTypeFilter,
                        currentResourceGroupFilter);
            }
            azureMetricDefinitionsCallCount.incrementAndGet();
            List<MetricDefinition> resourceMetrics = azure.metricDefinitions().listByResource(resource.id());
            try {
                generateMetrics(resourceMetrics);
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Skipping Resource {} of Type {} in Group {} because of Resource Filter {} of Type Filter {} in Group Filter {}",
                        resource.name(),
                        resource.resourceType(),
                        resource.resourceGroupName(),
                        currentResourceTypeFilter,
                        currentResourceTypeFilter,
                        currentResourceGroupFilter);
            }
        }
        if (finalMetricList.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Metric List is empty");
            }
        } else {
            metricWriteHelper.transformAndPrintMetrics(finalMetricList);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("azureMetricDefinitionsCallCount {}: {}", resource.id(), azureMetricDefinitionsCallCount);
            logger.debug("azureMetricsCallCount: " + azureMetricsCallCount);
        }
    }

    private void generateMetrics(List<MetricDefinition> resourceMetrics) throws MalformedURLException, UnsupportedEncodingException {
        List<Map<String, ?>> metricFilters = (List<Map<String, ?>>) resourceFilter.get("metrics");

        List<MetricDefinition> filteredMetrics = Lists.newArrayList();
        List<String> filteredMetricsNames = Lists.newArrayList();

        for (Map<String, ?> metricFilter : metricFilters) {
            for (MetricDefinition resourceMetric : resourceMetrics) {
                String currentMetricFilter = metricFilter.get("metric").toString();
                if (logger.isDebugEnabled()) {
                    logger.debug("resourceMetric name ({})", resourceMetric.name().value());
                    logger.debug("currentMetricFilter ({})", currentMetricFilter);
                    logger.debug("match ({})", resourceMetric.name().value().matches(currentMetricFilter), resourceMetric.name().value(), currentMetricFilter);
                }

                String apiEndpointBase = resourceMetric.id().substring(0,StringUtils.ordinalIndexOf(resourceMetric.id(), "/", 11));
                Object filterObject = metricFilter.get("filter");
                String filter = filterObject == null ? "" : filterObject.toString();

                if (resourceMetric.name().value().matches(currentMetricFilter)) {
                    if (!filter.isEmpty()) {
                        JsonNode apiResponse = getAzureMetrics(apiEndpointBase, resourceMetric.name().value(), filter);
                        if (apiResponse != null) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("API response: " + apiResponse.toString());
                            }
                            addMetric(resourceMetric, apiResponse, metricFilter);

                            return;
                        }
                    } else {
                        filteredMetrics.add(resourceMetric);
                        filteredMetricsNames.add(URLEncoder.encode(resourceMetric.name().value(), "UTF-8"));
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Not Reporting Metric {} for Resource {} as it is filtered by {}",
                                resourceMetric.name().value(),
                                resource.name(),
                                currentMetricFilter);
                    }
                }
            }
        }
        logger.debug("filteredMetrics size ({})", filteredMetrics.size());
        consumeAndImportAzureMetrics(filteredMetrics, filteredMetricsNames);
    }

    private void consumeAndImportAzureMetrics(List<MetricDefinition> filteredMetrics, List<String> filteredMetricsNames) throws MalformedURLException, UnsupportedEncodingException {
        if (!filteredMetrics.isEmpty()) {
            List<List<MetricDefinition>> filteredMetricsChunks = ListUtils.partition(filteredMetrics, Constants.AZURE_METRICS_CHUNK_SIZE);
            List<List<String>> filteredMetricsNamesChunks = ListUtils.partition(filteredMetricsNames, Constants.AZURE_METRICS_CHUNK_SIZE);
            Iterator<List<MetricDefinition>> filteredMetricsIterator = filteredMetricsChunks.iterator();
            Iterator<List<String>> filteredMetricsNamesIterator = filteredMetricsNamesChunks.iterator();

            String apiEndpointBase = filteredMetrics.get(0).id().substring(0,StringUtils.ordinalIndexOf(filteredMetrics.get(0).id(), "/", 11));

            while (filteredMetricsIterator.hasNext() && filteredMetricsNamesIterator.hasNext()) {
                List<MetricDefinition> filteredMetricsChunk = filteredMetricsIterator.next();
                List<String> filteredMetricsNamesChunk = filteredMetricsNamesIterator.next();
                JsonNode apiResponse = getAzureMetrics(apiEndpointBase, StringUtils.join(filteredMetricsNamesChunk, ','));
                if (apiResponse != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("API response: " + apiResponse.toString());
                    }
                    addMetrics(filteredMetricsChunk, apiResponse);
                }
            }
        }
    }

    private JsonNode getAzureMetrics(String apiEndpointBase, String metricNames) throws MalformedURLException, UnsupportedEncodingException {
        return getAzureMetrics(apiEndpointBase, metricNames, null);
    }

    private JsonNode getAzureMetrics(String apiEndpointBase, String metricNames,
        String filter) throws MalformedURLException, UnsupportedEncodingException {
        String url = Constants.AZURE_MANAGEMENT_URL
                + apiEndpointBase
                + "/metrics?timespan=" + recordDateTime.minusMinutes(2).toDateTimeISO() + "/" + recordDateTime.toDateTimeISO()
                + "&metricnames=" + metricNames
                + "&api-version=" + subscription.get("api-version");
        if (filter != null) {
            url += "&$filter=" + URLEncoder.encode(filter, "UTF-8");
        }
        URL apiEndpointFull = new URL(url);
        azureMetricsCallCount.incrementAndGet();
        JsonNode apiResponse = AzureRestOperation.doGet(Constants.azureAuthResult, apiEndpointFull);

        return apiResponse;
    }

    private void addMetrics(List<MetricDefinition> filteredMetricsChunk, JsonNode responseMetrics) {

        JsonNode responseMetricsValues = responseMetrics.get("value");
        Iterator<JsonNode> responseMetricsIterator = responseMetricsValues.elements();
        Iterator<MetricDefinition> filteredMetricsIterator = filteredMetricsChunk.iterator();
        while (responseMetricsIterator.hasNext() && filteredMetricsIterator.hasNext()) {
            MetricDefinition resourceMetric = filteredMetricsIterator.next();
            JsonNode responseMetric = responseMetricsIterator.next();
            if (logger.isDebugEnabled()) {
                logger.debug("Response metric: {}", responseMetric.toString());
            }
            addMetric(resourceMetric, responseMetric);
        }
    }

    private void addMetric(MetricDefinition resourceMetric, JsonNode responseMetric) {
        addMetric(resourceMetric, responseMetric, null);
    }

    private void addMetric(MetricDefinition resourceMetric, JsonNode responseMetric, Map<String, ?> metricFilter) {
        String metricAggregation = resourceMetric.primaryAggregationType().toString();
        String metricName = resourceMetric.name().value();
        String metricValue = null;
        String metricPath = metricPrefix +
                Constants.METRIC_SEPARATOR +
                subscriptionName +
                Constants.METRIC_SEPARATOR +
                resource.resourceGroupName() +
                Constants.METRIC_SEPARATOR +
                resource.resourceType() +
                Constants.METRIC_SEPARATOR +
                resource.name() +
                Constants.METRIC_SEPARATOR;
        if (logger.isDebugEnabled()) {
            logger.debug("Metric name / aggregation / path: {} / {} / {}", metricName, metricAggregation, metricPath);
        }

        JsonNode data = responseMetric.findPath("timeseries").findPath("data");
        switch (metricAggregation) {
            case "Count":
                metricValue = (data.path(0).path("count").isMissingNode()) ? null : data.get(0).get("count").toString();
                break;
            case "Average":
                metricValue = (data.path(0).path("average").isMissingNode()) ? null : data.get(0).get("average").toString();
                break;
            case "Total":
                metricValue = (data.path(0).path("total").isMissingNode()) ? null : data.get(0).get("total").toString();
                break;
            case "Maximum":
                metricValue = (data.path(0).path("maximum").isMissingNode()) ? null : data.get(0).get("maximum").toString();
                break;
            default:
                if (logger.isDebugEnabled()) {
                    logger.info("Not Reporting Metric {} for Resource {} as the aggregation type is not supported",
                            metricName,
                            resource.name());
                }
                break;
        }

        if (metricValue == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Ignoring Metric {} for Resource {} as it is null or empty",
                        metricName,
                        resource.name(),
                        metricValue);
            }
        } else {
            String subpath = "";
            Object aliasObject = null;
            Object subpathObject = null;
            if (metricFilter != null) {
                aliasObject = metricFilter.get("alias");
                subpathObject = metricFilter.get("subpath");
            }
            metricName = aliasObject == null ? metricName : aliasObject.toString();
            subpath = subpathObject == null ? "" : subpathObject.toString() + "|";
            Metric metric = new Metric(metricName, metricValue, metricPath + subpath + metricName);
            if (logger.isDebugEnabled()) {
                logger.debug("Reporting Metric {} for Resource {} with value {}",
                        metricName,
                        resource.name(),
                        metricValue);
            }
            finalMetricList.add(metric);
        }
    }
}
