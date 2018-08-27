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
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.util.AssertUtils;
import com.appdynamics.monitors.azure.utils.AzureAPIWrapper;
import com.appdynamics.monitors.azure.utils.Constants;
import com.appdynamics.monitors.azure.utils.MetricDefinition;
import com.appdynamics.monitors.azure.utils.Resource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.mockito.Mockito.*;

public class AzureMonitorTest {
    // --Commented out by Inspection (7/4/18 6:16 PM):
    private static final Logger logger = LoggerFactory.getLogger(AzureMonitorTest.class);

//    @Test
//    public void testAzureMonitor(){
//        Map<String, String> taskArgs = new HashMap<>();
//        taskArgs.put("config-file", "src/test/resources/conf/integration-test-config.yml");
//        try {
//            testAzureMonitorRun(taskArgs);
//        } catch (TaskExecutionException e) {
//            e.printStackTrace();
//        }
//    }

//    @Test
//    public void testAzureMonitorWithEncryption(){
//        Map<String, String> taskArgs = new HashMap<>();
//        taskArgs.put("config-file", "src/test/resources/conf/integration-test-encrypted-config.yml");
//        try {
//            testAzureMonitorRun(taskArgs);
//        } catch (TaskExecutionException e) {
//            e.printStackTrace();
//        }
//    }

    @Test
    public void testAzureMonitorTaskWithMetricsWithoutDimensionsRun(){
        try {
            testAzureMonitorTaskWithMetricsWithoutDimensionsRun("src/test/resources/conf/integration-test-config.yml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
//
//    @Test
//    public void testAzureMonitorTaskWithEncryption(){
//        try {
//            testAzureMonitorTaskRun("src/test/resources/conf/integration-test-encrypted-config.yml");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Test
//    public void testAzureMonitorTaskWithKeyvault(){
//        try {
//            testAzureMonitorTaskRun("src/test/resources/conf/integration-test-keyvault-config.yml");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Test
//    public void testAzureMonitorTaskWithKeyvaultWithEncryption(){
//        try {
//            testAzureMonitorTaskRun("src/test/resources/conf/integration-test-keyvault-encrypted-config.yml");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Test
//    public void testResourceFilters(){
//        try {
//            testAzureMonitorTaskRun("src/test/resources/conf/integration-test-resourcefilter-config.yml");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void testAzureMonitorRun(Map<String, String> taskArgs) throws TaskExecutionException {
//        new AzureMonitor().execute(taskArgs, null);
//    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    private void testAzureMonitorTaskWithMetricsWithoutDimensionsRun(String configYml) {
        AMonitorJob aMonitorJob = mock(AMonitorJob.class);
        MetricWriteHelper metricWriteHelper = mock(MetricWriteHelper.class);
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                List<Metric> metrics = (List<Metric>) invocation.getArguments()[0];
                Object mock = invocation.getMock();
                for (Object metric : metrics) {
                    logger.debug("Printing metric " + metric.toString());
                }
                return null;
              }
           })
        .when(metricWriteHelper)
        .transformAndPrintMetrics(Mockito.any(List.class));

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
            AzureAPIWrapper azure = mockAzureAPIWrapperWithMetricsAndWithoutDimensions(subscription);
            AzureMonitorTask task = new AzureMonitorTask(monitorContextConfiguration, metricWriteHelper, subscription, countDownLatch, azure);
            monitorContextConfiguration.getContext().getExecutorService().execute("Azure Monitor", task);
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private AzureAPIWrapper mockAzureAPIWrapperWithMetricsAndWithoutDimensions(Map<String, String> subscription) {
        AzureAPIWrapper azure = mock(AzureAPIWrapper.class);

        List<Resource> resources = Lists.newArrayList();
        Resource resource1 = new Resource();
        resource1.setId("/subscriptions/39eee2d9-0ea1-4e3c-93bc-3619f821acb1/resourceGroups/AppDTest/providers/Microsoft.ApiManagement/service/CoBaApiManagement1");
        resource1.setName("CoBaApiManagement1");
        resource1.setResourceGroupName("AppDTest");
        resource1.setResourceType("service");
        resource1.setType("Microsoft.ApiManagement/service");
        resources.add(resource1);
        when(azure.getResources()).thenReturn(resources);

        List<MetricDefinition> metricDefinitions = Lists.newArrayList();
        MetricDefinition metricDefinition1 = new MetricDefinition();
        metricDefinition1.setId("/subscriptions/39eee2d9-0ea1-4e3c-93bc-3619f821acb1/resourceGroups/AppDTest/providers/Microsoft.ApiManagement/service/CoBaApiManagement1/providers/microsoft.insights/metricdefinitions/SuccessfulRequests");
        metricDefinition1.setName("SuccessfulRequests");
        metricDefinition1.setPrimaryAggregationType("Total");
        metricDefinitions.add(metricDefinition1);
        MetricDefinition metricDefinition2 = new MetricDefinition();
        metricDefinition2.setId("/subscriptions/39eee2d9-0ea1-4e3c-93bc-3619f821acb1/resourceGroups/AppDTest/providers/Microsoft.ApiManagement/service/CoBaApiManagement1/providers/microsoft.insights/metricdefinitions/FailedRequests");
        metricDefinition2.setName("FailedRequests");
        metricDefinition2.setPrimaryAggregationType("Total");
        metricDefinitions.add(metricDefinition2);
        MetricDefinition metricDefinition3 = new MetricDefinition();
        metricDefinition3.setId("/subscriptions/39eee2d9-0ea1-4e3c-93bc-3619f821acb1/resourceGroups/AppDTest/providers/Microsoft.ApiManagement/service/CoBaApiManagement1/providers/microsoft.insights/metricdefinitions/UnauthorizedRequests");
        metricDefinition3.setName("UnauthorizedRequests");
        metricDefinition3.setPrimaryAggregationType("Total");
        metricDefinitions.add(metricDefinition3);
        when(azure.getMetricDefinitions(resource1.getId())).thenReturn(metricDefinitions);

        ObjectMapper objectMapper = new ObjectMapper();
        String apiResponse = "{\"cost\":0,\"timespan\":\"2018-08-27T15:58:01Z/2018-08-27T16:00:01Z\",\"interval\":\"PT1M\",\"value\":[{\"id\":\"/subscriptions/39eee2d9-0ea1-4e3c-93bc-3619f821acb1/resourceGroups/AppDTest/providers/Microsoft.ApiManagement/service/CoBaApiManagement1/providers/Microsoft.Insights/metrics/SuccessfulRequests\",\"type\":\"Microsoft.Insights/metrics\",\"name\":{\"value\":\"SuccessfulRequests\",\"localizedValue\":\"Successful Gateway Requests\"},\"unit\":\"Count\",\"timeseries\":[{\"metadatavalues\":[],\"data\":[{\"timeStamp\":\"2018-08-27T15:58:00Z\",\"total\":0.0},{\"timeStamp\":\"2018-08-27T15:59:00Z\",\"total\":0.0}]}]},{\"id\":\"/subscriptions/39eee2d9-0ea1-4e3c-93bc-3619f821acb1/resourceGroups/AppDTest/providers/Microsoft.ApiManagement/service/CoBaApiManagement1/providers/Microsoft.Insights/metrics/FailedRequests\",\"type\":\"Microsoft.Insights/metrics\",\"name\":{\"value\":\"FailedRequests\",\"localizedValue\":\"Failed Gateway Requests\"},\"unit\":\"Count\",\"timeseries\":[{\"metadatavalues\":[],\"data\":[{\"timeStamp\":\"2018-08-27T15:58:00Z\",\"total\":0.0},{\"timeStamp\":\"2018-08-27T15:59:00Z\",\"total\":0.0}]}]},{\"id\":\"/subscriptions/39eee2d9-0ea1-4e3c-93bc-3619f821acb1/resourceGroups/AppDTest/providers/Microsoft.ApiManagement/service/CoBaApiManagement1/providers/Microsoft.Insights/metrics/UnauthorizedRequests\",\"type\":\"Microsoft.Insights/metrics\",\"name\":{\"value\":\"UnauthorizedRequests\",\"localizedValue\":\"Unauthorized Gateway Requests\"},\"unit\":\"Count\",\"timeseries\":[{\"metadatavalues\":[],\"data\":[{\"timeStamp\":\"2018-08-27T15:58:00Z\",\"total\":0.0},{\"timeStamp\":\"2018-08-27T15:59:00Z\",\"total\":0.0}]}]},{\"id\":\"/subscriptions/39eee2d9-0ea1-4e3c-93bc-3619f821acb1/resourceGroups/AppDTest/providers/Microsoft.ApiManagement/service/CoBaApiManagement1/providers/Microsoft.Insights/metrics/Duration\",\"type\":\"Microsoft.Insights/metrics\",\"name\":{\"value\":\"Duration\",\"localizedValue\":\"Overall Duration of Gateway Requests\"},\"unit\":\"MilliSeconds\",\"timeseries\":[{\"metadatavalues\":[],\"data\":[{\"timeStamp\":\"2018-08-27T15:58:00Z\",\"total\":0.0},{\"timeStamp\":\"2018-08-27T15:59:00Z\",\"total\":0.0}]}]}],\"namespace\":\"Microsoft.ApiManagement/service\",\"resourceregion\":\"northeurope\"}";
        JsonNode metrics = null;
        try {
            metrics = objectMapper.readTree(apiResponse);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            when(azure.getMetrics(metricDefinition1, "SuccessfulRequests,FailedRequests,UnauthorizedRequests")).thenReturn(metrics);
        } catch (MalformedURLException | UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return azure;
    }
}
