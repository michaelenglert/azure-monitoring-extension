/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.monitors.azure;

import com.appdynamics.extensions.*;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.util.AssertUtils;
import com.appdynamics.monitors.azure.utils.Constants;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Test;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.mockito.Mockito.mock;

public class AzureMonitorTest {
    // --Commented out by Inspection (7/4/18 6:16 PM):private static final Logger logger = LoggerFactory.getLogger(AzureMonitorTest.class);

    @Test
    public void testAzureMonitor(){
        Map<String, String> taskArgs = new HashMap<>();
        taskArgs.put("config-file", "src/test/resources/conf/integration-test-config.yml");
        try {
            testAzureMonitorRun(taskArgs);
        } catch (TaskExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAzureMonitorWithEncryption(){
        Map<String, String> taskArgs = new HashMap<>();
        taskArgs.put("config-file", "src/test/resources/conf/integration-test-encrypted-config.yml");
        try {
            testAzureMonitorRun(taskArgs);
        } catch (TaskExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAzureMonitorTask(){
        try {
            testAzureMonitorTaskRun("src/test/resources/conf/integration-test-config.yml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAzureMonitorTaskWithEncryption(){
        try {
            testAzureMonitorTaskRun("src/test/resources/conf/integration-test-encrypted-config.yml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAzureMonitorTaskWithKeyvault(){
        try {
            testAzureMonitorTaskRun("src/test/resources/conf/integration-test-keyvault-config.yml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAzureMonitorTaskWithKeyvaultWithEncryption(){
        try {
            testAzureMonitorTaskRun("src/test/resources/conf/integration-test-keyvault-encrypted-config.yml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testResourceFilters(){
        try {
            testAzureMonitorTaskRun("src/test/resources/conf/integration-test-resourcefilter-config.yml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void testAzureMonitorRun(Map<String, String> taskArgs) throws TaskExecutionException {
        new AzureMonitor().execute(taskArgs, null);
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    private void testAzureMonitorTaskRun(String configYml) {
        AMonitorJob aMonitorJob = mock(AMonitorJob.class);
        MetricWriteHelper metricWriteHelper = mock(MetricWriteHelper.class);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        MonitorContextConfiguration monitorContextConfiguration = new MonitorContextConfiguration(
                "AzureMonitor",
                Constants.DEFAULT_METRIC_PREFIX,
                new File(System.getProperty("user.dir")),
                aMonitorJob);
        monitorContextConfiguration.setConfigYml(configYml);
        List<Map<String,String>> subscriptions = (List<Map<String,String>>)monitorContextConfiguration.getConfigYml().get("subscriptions");
        AssertUtils.assertNotNull(subscriptions, "The 'subscriptions' section in config.yml is not initialised");
        for (Map<String, String> subscription : subscriptions) {
            AzureMonitorTask task = new AzureMonitorTask(monitorContextConfiguration, metricWriteHelper, subscription, countDownLatch);
            monitorContextConfiguration.getContext().getExecutorService().execute("Azure Monitor", task);
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
