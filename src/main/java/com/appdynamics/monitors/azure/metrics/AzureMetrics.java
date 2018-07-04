/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.monitors.azure.metrics;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.monitors.azure.utils.Constants;
import com.google.common.collect.Lists;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.monitor.MetricCollection;
import com.microsoft.azure.management.monitor.MetricDefinition;
import com.microsoft.azure.management.resources.GenericResource;
import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@SuppressWarnings("unchecked")
public class AzureMetrics implements AMonitorTaskRunnable {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AzureMetrics.class);
    private final Map<String, ?> resourceFilter;
    private final String currentResourceGroupFilter;
    private final String currentResourceFilter;
    private final String currentResourceTypeFilter;
    private final GenericResource resource;
    private final String subscriptionName;
    private final CountDownLatch countDownLatch;
    private final MetricWriteHelper metricWriteHelper;
    private final Azure azure;
    private final String metricPrefix;

    public AzureMetrics(Map<String, ?> resourceFilter,
                        String currentResourceGroupFilter,
                        String currentResourceFilter,
                        String currentResourceTypeFilter,
                        GenericResource resource,
                        String subscriptionName,
                        CountDownLatch countDownLatch,
                        MetricWriteHelper metricWriteHelper,
                        Azure azure,
                        String metricPrefix) {
        this.resourceFilter = resourceFilter;
        this.currentResourceGroupFilter = currentResourceGroupFilter;
        this.currentResourceFilter = currentResourceFilter;
        this.currentResourceTypeFilter = currentResourceTypeFilter;
        this.resource = resource;
        this.subscriptionName = subscriptionName;
        this.countDownLatch = countDownLatch;
        this.metricWriteHelper = metricWriteHelper;
        this.azure = azure;
        this.metricPrefix = metricPrefix;
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
        List<Metric> finalMetricList = Lists.newArrayList();
        DateTime recordDateTime = DateTime.now();
        if (resource.name().matches(currentResourceFilter) &&
                resource.resourceType().matches(currentResourceTypeFilter) &&
                resource.resourceGroupName().matches(currentResourceGroupFilter)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Working on Resource {} of Type {} in Group {} because of Resource Filter {} of Type Filter {} in Group Filter {}",
                        resource.name(),
                        resource.resourceType(),
                        resource.resourceGroupName(),
                        currentResourceTypeFilter,
                        currentResourceTypeFilter,
                        currentResourceGroupFilter);
            }
            List<MetricDefinition> resourceMetrics = azure.metricDefinitions().listByResource(resource.id());
            List<Map<String, String>> metricFilters = (List<Map<String, String>>) resourceFilter.get("metrics");

            for (Map<String, String> metricFilter : metricFilters) {
                for (MetricDefinition resourceMetric : resourceMetrics) {
                    String currentMetricFilter = metricFilter.get("metric");
                    if (resourceMetric.isDimensionRequired()) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Resource Metric {} needs Dimensions. This is currently not supported",
                                    resourceMetric.id());
                        }
                    } else if (resourceMetric.name().value().matches(currentMetricFilter)) {
                        MetricCollection metricCollection = resourceMetric.defineQuery().startingFrom(recordDateTime.minusMinutes(2)).endsBefore(recordDateTime).execute();
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
                        switch (metricAggregation) {
                            case "Count":
                                metricValue = String.valueOf(metricCollection.metrics().get(0).timeseries().get(0).data().get(0).count());
                                break;
                            case "Average":
                                metricValue = String.valueOf(metricCollection.metrics().get(0).timeseries().get(0).data().get(0).average());
                                break;
                            case "Total":
                                metricValue = String.valueOf(metricCollection.metrics().get(0).timeseries().get(0).data().get(0).total());
                                break;
                            case "Maximum":
                                metricValue = String.valueOf(metricCollection.metrics().get(0).timeseries().get(0).data().get(0).maximum());
                                break;
                            default:
                                logger.info("Not Reporting Metric {} for Resource {} as the aggregation type is not supported",
                                        metricName,
                                        resource.name());
                                break;
                        }
                        assert metricValue != null;
                        if (metricValue.isEmpty() || metricValue.equals("0.0") || metricValue.equals("null")) {
                            logger.debug("Ignoring Metric {} for Resource {} as it is null or empty",
                                    metricName,
                                    resource.name(),
                                    metricValue);
                        } else {
                            Metric metric = new Metric(metricName, metricValue, metricPath + metricName);
                            if (logger.isDebugEnabled()) {
                                logger.debug("Reporting Metric {} for Resource {} with value {}",
                                        metricName,
                                        resource.name(),
                                        metricValue);
                            }
                            finalMetricList.add(metric);
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
    }
}
