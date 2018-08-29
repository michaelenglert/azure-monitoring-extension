/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.monitors.azure;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.monitors.azure.metrics.AzureMetrics;
import com.appdynamics.monitors.azure.utils.AzureAPIWrapper;
import com.appdynamics.monitors.azure.utils.Constants;
import com.appdynamics.monitors.azure.utils.MetricDefinition;
import com.appdynamics.monitors.azure.utils.Resource;

import org.apache.commons.collections4.ListUtils;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
class AzureMonitorTask implements AMonitorTaskRunnable{
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AzureMonitorTask.class);

    private static long startTime;

    private final MonitorContextConfiguration configuration;
    private final MetricWriteHelper metricWriteHelper;
    private final Map<String, ?> subscription;
    private final List<Map<String,?>>  resourceGroupFilters;
    private final CountDownLatch countDownLatch;

    private AzureAPIWrapper azure;

    AzureMonitorTask(MonitorContextConfiguration configuration, MetricWriteHelper metricWriteHelper, Map<String, ?> subscription, CountDownLatch countDownLatch, AzureAPIWrapper azure) {
        this.configuration = configuration;
        this.metricWriteHelper = metricWriteHelper;
        this.subscription = subscription;
        this.countDownLatch = countDownLatch;
        this.resourceGroupFilters = (List<Map<String,?>>) subscription.get("resourceGroups");
        this.azure = azure;
//        AssertUtils.assertNotNull(resourceGroupFilters, "The 'resourceGroupFilters' section in config.yml is either null or empty");
    }

    @Override
    public void onTaskComplete() {
        logger.info("Task Completed for subscription {}", subscription.get("subscriptionId").toString());
        long finishTime = System.currentTimeMillis();
        long totalTime = finishTime - startTime;
        logger.debug("Total time: " + (totalTime / 1000.0f) + " ms");
//        logger.debug("azureResourcesCallCount: " + this.azureResourcesCallCount);
    }

    @Override
    public void run() {
        try {
            startTime = System.currentTimeMillis();
            runTask();
        }
        catch(Exception e){
            logger.error(e.getMessage());
        }
        finally {
            countDownLatch.countDown();
        }
    }
    private void runTask() throws Exception{
        azure.authorize();
        List<Resource> resources = azure.getResources();
        if (resources == null || resources.size() == 0) throw new Exception("Resources list is null or empty");
        for (Map<String, ?> resourceGroupFilter : resourceGroupFilters) {
            List<Map<String, ?>> resourceTypeFilters = (List<Map<String, ?>>) resourceGroupFilter.get("resourceTypes");
            for (Map<String, ?> resourceTypeFilter : resourceTypeFilters) {
                List<Map<String, ?>> resourceFilters = (List<Map<String, ?>>) resourceTypeFilter.get("resources");
                for (Map<String, ?> resourceFilter : resourceFilters) {
                    String currentResourceGroupFilter = resourceGroupFilter.get("resourceGroup").toString();
                    List<List<Resource>> resourcesChunks = ListUtils.partition(resources, (int) (.75 * (int) configuration.getConfigYml().get("numberOfThreads")));
                    for (List<Resource> resourcesChunk : resourcesChunks) {
                        CountDownLatch countDownLatchAzure = new CountDownLatch(resourcesChunk.size());
                        for (Resource resource : resourcesChunk) {
                            String currentResourceFilter = resourceFilter.get("resource").toString();
                            String currentResourceTypeFilter = resourceTypeFilter.get("resourceType").toString();
                            AzureMetrics azureMetricsTask = new AzureMetrics(
                                    azure,
                                    resourceFilter,
                                    currentResourceGroupFilter,
                                    currentResourceFilter,
                                    currentResourceTypeFilter,
                                    resource,
                                    subscription,
                                    countDownLatchAzure,
                                    metricWriteHelper,
                                    configuration.getMetricPrefix());
    
                            configuration.getContext().getExecutorService().submit(resource.getName(), azureMetricsTask);
                        }
                        try{
                            countDownLatchAzure.await(Constants.TASKS_COUNTDOWN_LATCH_TIMEOUT, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}