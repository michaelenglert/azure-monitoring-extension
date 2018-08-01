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

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.util.AssertUtils;
import com.appdynamics.monitors.azure.utils.Constants;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

@SuppressWarnings({"WeakerAccess", "unchecked"})
public class AzureMonitor extends ABaseMonitor {

    @Override
    protected String getDefaultMetricPrefix() {
        return Constants.DEFAULT_METRIC_PREFIX;
    }

    @Override
    public String getMonitorName() {
        return "Azure Monitoring Extension";
    }

    @Override
    protected void doRun(TasksExecutionServiceProvider tasksExecutionServiceProvider) {
        List<Map<String,?>> subscriptions = (List<Map<String,?>>)getContextConfiguration().getConfigYml().get("subscriptions");
        AssertUtils.assertNotNull(subscriptions, "The 'subscriptions' section in config.yml is not initialised");
        for (Map<String, ?> subscription : subscriptions) {
            AzureMonitorTask task = new AzureMonitorTask(getContextConfiguration(), tasksExecutionServiceProvider.getMetricWriteHelper(), subscription, new CountDownLatch(getTaskCount()));
            tasksExecutionServiceProvider.submit(subscription.get("subscriptionId").toString(),task);
        }
    }

    @Override
    protected int getTaskCount() {
        List<Map<String,?>> subscriptions = (List<Map<String,?>>)getContextConfiguration().getConfigYml().get("subscriptions");
        List<Map<String,?>> serviceFabrics = (List<Map<String,?>>)getContextConfiguration().getConfigYml().get("serviceFabrics");
        AssertUtils.assertNotNull(subscriptions, "The 'subscriptions' section in config.yml is not initialised");
        AssertUtils.assertNotNull(serviceFabrics, "The 'serviceFabrics' section in config.yml is not initialised");
        return subscriptions.size() + serviceFabrics.size();
    }

    public static void main(String[] args) throws TaskExecutionException {

        AzureMonitor monitor = new AzureMonitor();

        Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put("config-file","src/test/resources/conf/integration-test-config.yml");
        monitor.execute(taskArgs, null);
    }
}
