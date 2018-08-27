/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.monitors.azure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.util.AssertUtils;
import com.appdynamics.monitors.azure.utils.AzureAPIWrapper;
import com.appdynamics.monitors.azure.utils.Constants;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

@SuppressWarnings({"WeakerAccess", "unchecked"})
public class AzureMonitor extends ABaseMonitor {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AzureMonitorTask.class);

    @Override
    protected String getDefaultMetricPrefix() {
        return Constants.DEFAULT_METRIC_PREFIX;
    }

    @Override
    public String getMonitorName() {
        return "Azure Monitoring Extension";
    }

    @Override
    public void onComplete() {
        logger.info("Monitor Completed");
    }

    @Override
    protected void doRun(TasksExecutionServiceProvider tasksExecutionServiceProvider) {
        List<Map<String,?>> subscriptions = (List<Map<String,?>>)getContextConfiguration().getConfigYml().get("subscriptions");
        AssertUtils.assertNotNull(subscriptions, "The 'subscriptions' section in config.yml is not initialised");
        CountDownLatch countDownLatch = new CountDownLatch(getTaskCount());
        for (Map<String, ?> subscription : subscriptions) {
            AzureAPIWrapper azure = new AzureAPIWrapper(subscription);
			AzureMonitorTask task = new AzureMonitorTask(getContextConfiguration(), tasksExecutionServiceProvider.getMetricWriteHelper(), subscription, countDownLatch, azure);
            tasksExecutionServiceProvider.submit(subscription.get("subscriptionId").toString(),task);
        }
        try{
            countDownLatch.await(45, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected int getTaskCount() {
        List<Map<String,?>> subscriptions = (List<Map<String,?>>)getContextConfiguration().getConfigYml().get("subscriptions");
        // List<Map<String,?>> serviceFabrics = (List<Map<String,?>>)getContextConfiguration().getConfigYml().get("serviceFabrics");
        AssertUtils.assertNotNull(subscriptions, "The 'subscriptions' section in config.yml is not initialised");
        // AssertUtils.assertNotNull(serviceFabrics, "The 'serviceFabrics' section in config.yml is not initialised");
//        return subscriptions.size() + serviceFabrics.size();
        return subscriptions.size();
    }

    public static void main(String[] args) throws TaskExecutionException {

        AzureMonitor monitor = new AzureMonitor();

        Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put("config-file",Constants.TEST_CONFIG_FILE);
        monitor.execute(taskArgs, null);
    }
}
