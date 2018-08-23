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
import com.appdynamics.extensions.util.AssertUtils;
import com.appdynamics.monitors.azure.metrics.AzureMetrics;
import com.appdynamics.monitors.azure.utils.Constants;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.GenericResource;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unchecked")
class AzureMonitorTask implements AMonitorTaskRunnable{
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AzureMonitorTask.class);

    private final AtomicInteger azureResourcesCallCount = new AtomicInteger(0);

    private static long startTime = System.currentTimeMillis();

    private final MonitorContextConfiguration configuration;
    private final MetricWriteHelper metricWriteHelper;
    private final Map<String, ?> subscription;
    private final List<Map<String,?>>  resourceGroupFilters;
    private final CountDownLatch countDownLatch;

    AzureMonitorTask(MonitorContextConfiguration configuration, MetricWriteHelper metricWriteHelper, Map<String, ?> subscription, CountDownLatch countDownLatch) {
         this.configuration = configuration;
         this.metricWriteHelper = metricWriteHelper;
         this.subscription = subscription;
         this.countDownLatch = countDownLatch;
         resourceGroupFilters = (List<Map<String,?>>) subscription.get("resourceGroups");
         AssertUtils.assertNotNull(resourceGroupFilters, "The 'resourceGroupFilters' section in config.yml is either null or empty");
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
        AzureAuth.getAzureAuth(subscription);
        Azure azure = Constants.azureMonitorAuth.withSubscription(subscription.get("subscriptionId").toString());
        this.azureResourcesCallCount.incrementAndGet();
        PagedList<GenericResource> resources = azure.genericResources().list();
        for (Map<String, ?> resourceGroupFilter : resourceGroupFilters) {
            List<Map<String, ?>> resourceTypeFilters = (List<Map<String, ?>>) resourceGroupFilter.get("resourceTypes");
            for (Map<String, ?> resourceTypeFilter : resourceTypeFilters) {
                List<Map<String, ?>> resourceFilters = (List<Map<String, ?>>) resourceTypeFilter.get("resources");
                for (Map<String, ?> resourceFilter : resourceFilters) {
                    String currentResourceGroupFilter = resourceGroupFilter.get("resourceGroup").toString();
                    CountDownLatch countDownLatchAzure = new CountDownLatch(resources.size());
                    for (GenericResource resource : resources) {
                        String currentResourceFilter = resourceFilter.get("resource").toString();
                        String currentResourceTypeFilter = resourceTypeFilter.get("resourceType").toString();
                        AzureMetrics azureMetricsTask = new AzureMetrics(
                                resourceFilter,
                                currentResourceGroupFilter,
                                currentResourceFilter,
                                currentResourceTypeFilter,
                                resource,
                                subscription,
                                countDownLatchAzure,
                                metricWriteHelper,
                                azure,
                                configuration.getMetricPrefix());

                        configuration.getContext().getExecutorService().execute("AzureMetrics", azureMetricsTask);
                    }
                    try{
                        countDownLatchAzure.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}