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

import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class AzureMonitorTest {
    private static final Logger logger = LoggerFactory.getLogger(AzureMonitorTest.class);
    private static MonitorContextConfiguration monitorContextConfiguration;
    private static MetricWriteHelper metricWriteHelper;
    private static Map<String, String> subscription;
    private static String metricPrefix;
    private List<Resource> resources;
    private List<MetricDefinition> metricDefinitions;
    private String metricsApiResponse;
    private String metricNames;
    private String filter;
    private String expectedOutput;

    public AzureMonitorTest(List<Resource> resources, List<MetricDefinition> metricDefinitions, String metricsApiResponse, String metricNames, String filter, String expectedOutput) {
        this.resources = resources;
        this.expectedOutput = expectedOutput;
        this.metricDefinitions = metricDefinitions;
        this.metricsApiResponse = metricsApiResponse;
        this.metricNames = metricNames;
        this.filter = filter;
    }

    @Parameters
    public static Collection<Object[]> data() {
        AMonitorJob aMonitorJob = mock(AMonitorJob.class);
        monitorContextConfiguration = new MonitorContextConfiguration(
                "AzureMonitor",
                Constants.DEFAULT_METRIC_PREFIX,
                new File(System.getProperty("user.dir")),
                aMonitorJob);
        metricWriteHelper = mock(MetricWriteHelper.class);
        metricPrefix = "Custom Metrics|AzureMonitor|";

        String configYml = "src/test/resources/conf/integration-test-config.yml";
        monitorContextConfiguration.setConfigYml(configYml);
        List<Map<String,String>> subscriptions = (List<Map<String,String>>) monitorContextConfiguration.getConfigYml().get("subscriptions");
        AssertUtils.assertNotNull(subscriptions, "The 'subscriptions' section in config.yml is not initialised");

        Iterator<Map<String, String>> iter = subscriptions.iterator();
        subscription = iter.next();
        String subscriptionName = "";
        if (subscription.containsKey("subscriptionName")){
            subscriptionName = subscription.get("subscriptionName").toString();
        }
        else {
            subscriptionName = subscription.get("subscriptionId").toString();
        }
        
        List<Resource> resourcesWithMetrics = Lists.newArrayList();
        Resource resourceWithMetrics = new Resource();
        resourceWithMetrics.setId("/subscriptions/1a2b3c4b-e5f6-g7h8-a123-1a23bcde456f/resourceGroups/AppDTest/providers/Microsoft.ApiManagement/service/TestApiManagement1");
        resourceWithMetrics.setName("TestApiManagement1");
        resourceWithMetrics.setResourceGroupName("AppDTest");
        resourceWithMetrics.setResourceType("service");
        resourceWithMetrics.setType("Microsoft.ApiManagement/service");
        resourcesWithMetrics.add(resourceWithMetrics);
        
        List<Resource> resourcesWithQualifier = Lists.newArrayList();
        Resource resourceWithQualifier = new Resource();
        resourceWithQualifier.setId("/subscriptions/1a2b3c4b-e5f6-g7h8-a123-1a23bcde456f/resourceGroups/AppDTest/providers/Microsoft.ApiManagement/service/TestApiManagement2");
        resourceWithQualifier.setName("TestApiManagement2");
        resourceWithQualifier.setResourceGroupName("AppDTest");
        resourceWithQualifier.setResourceType("service");
        resourceWithQualifier.setType("Microsoft.ApiManagement/service");
        resourcesWithQualifier.add(resourceWithQualifier);
        
        List<Resource> resourcesWithDimensions = Lists.newArrayList();
        Resource resourceWithDimensions = new Resource();
        resourceWithDimensions.setId("/subscriptions/1a2b3c4b-e5f6-g7h8-a123-1a23bcde456f/resourceGroups/AppDTest/providers/Microsoft.EventHub/namespaces/TestHub1");
        resourceWithDimensions.setName("TestHub1");
        resourceWithDimensions.setResourceGroupName("AppDTest");
        resourceWithDimensions.setResourceType("namespaces");
        resourceWithDimensions.setType("Microsoft.EventHub/namespaces");
        resourcesWithDimensions.add(resourceWithDimensions);

        List<MetricDefinition> metricDefinitionsWithMetrics = Lists.newArrayList();
        MetricDefinition metricDefinitionSuccessfulRequests = new MetricDefinition();
        metricDefinitionSuccessfulRequests.setId("/subscriptions/1a2b3c4b-e5f6-g7h8-a123-1a23bcde456f/resourceGroups/AppDTest/providers/Microsoft.ApiManagement/service/TestApiManagement1/providers/microsoft.insights/metricdefinitions/SuccessfulRequests");
        metricDefinitionSuccessfulRequests.setName("SuccessfulRequests");
        metricDefinitionSuccessfulRequests.setPrimaryAggregationType("Total");
        metricDefinitionsWithMetrics.add(metricDefinitionSuccessfulRequests);
        MetricDefinition metricDefinitionFailedRequests = new MetricDefinition();
        metricDefinitionFailedRequests.setId("/subscriptions/1a2b3c4b-e5f6-g7h8-a123-1a23bcde456f/resourceGroups/AppDTest/providers/Microsoft.ApiManagement/service/TestApiManagement1/providers/microsoft.insights/metricdefinitions/FailedRequests");
        metricDefinitionFailedRequests.setName("FailedRequests");
        metricDefinitionFailedRequests.setPrimaryAggregationType("Total");
        metricDefinitionsWithMetrics.add(metricDefinitionFailedRequests);
        MetricDefinition metricDefinitionUnauthorizedRequests = new MetricDefinition();
        metricDefinitionUnauthorizedRequests.setId("/subscriptions/1a2b3c4b-e5f6-g7h8-a123-1a23bcde456f/resourceGroups/AppDTest/providers/Microsoft.ApiManagement/service/TestApiManagement1/providers/microsoft.insights/metricdefinitions/UnauthorizedRequests");
        metricDefinitionUnauthorizedRequests.setName("UnauthorizedRequests");
        metricDefinitionUnauthorizedRequests.setPrimaryAggregationType("Total");
        metricDefinitionsWithMetrics.add(metricDefinitionUnauthorizedRequests);
        List<MetricDefinition> metricDefinitionsWithQualifier = Lists.newArrayList();
        MetricDefinition metricDefinitionUnauthorizedRequestsWithQualifier = new MetricDefinition();
        metricDefinitionUnauthorizedRequestsWithQualifier.setId("/subscriptions/1a2b3c4b-e5f6-g7h8-a123-1a23bcde456f/resourceGroups/AppDTest/providers/Microsoft.ApiManagement/service/TestApiManagement2/providers/microsoft.insights/metricdefinitions/UnauthorizedRequests");
        metricDefinitionUnauthorizedRequestsWithQualifier.setName("UnauthorizedRequests");
        metricDefinitionUnauthorizedRequestsWithQualifier.setPrimaryAggregationType("Total");
        metricDefinitionsWithQualifier.add(metricDefinitionUnauthorizedRequestsWithQualifier);

        List<MetricDefinition> metricDefinitionsWithDimensions = Lists.newArrayList();
        MetricDefinition metricDefinitionThrottledRequests = new MetricDefinition();
        metricDefinitionThrottledRequests.setId("/subscriptions/1a2b3c4b-e5f6-g7h8-a123-1a23bcde456f/resourceGroups/AppDTest/providers/Microsoft.EventHub/namespaces/TestHub1/providers/microsoft.insights/metricdefinitions/ThrottledRequests");
        metricDefinitionThrottledRequests.setName("ThrottledRequests");
        metricDefinitionThrottledRequests.setPrimaryAggregationType("Total");
        metricDefinitionsWithDimensions.add(metricDefinitionThrottledRequests);

        String apiResponseWithMetrics = "{\"cost\":0,\"timespan\":\"2018-08-28T09:02:19Z/2018-08-28T09:04:19Z\",\"interval\":\"PT1M\",\"value\":[{\"id\":\"/subscriptions/1a2b3c4b-e5f6-g7h8-a123-1a23bcde456f/resourceGroups/AppDTest/providers/Microsoft.ApiManagement/service/TestApiManagement1/providers/Microsoft.Insights/metrics/SuccessfulRequests\",\"type\":\"Microsoft.Insights/metrics\",\"name\":{\"value\":\"SuccessfulRequests\",\"localizedValue\":\"Successful Gateway Requests\"},\"unit\":\"Count\",\"timeseries\":[{\"metadatavalues\":[],\"data\":[{\"timeStamp\":\"2018-08-28T09:02:00Z\",\"total\":0.0},{\"timeStamp\":\"2018-08-28T09:03:00Z\",\"total\":0.0}]}]},{\"id\":\"/subscriptions/1a2b3c4b-e5f6-g7h8-a123-1a23bcde456f/resourceGroups/AppDTest/providers/Microsoft.ApiManagement/service/TestApiManagement1/providers/Microsoft.Insights/metrics/FailedRequests\",\"type\":\"Microsoft.Insights/metrics\",\"name\":{\"value\":\"FailedRequests\",\"localizedValue\":\"Failed Gateway Requests\"},\"unit\":\"Count\",\"timeseries\":[{\"metadatavalues\":[],\"data\":[{\"timeStamp\":\"2018-08-28T09:02:00Z\",\"total\":0.0},{\"timeStamp\":\"2018-08-28T09:03:00Z\",\"total\":0.0}]}]},{\"id\":\"/subscriptions/1a2b3c4b-e5f6-g7h8-a123-1a23bcde456f/resourceGroups/AppDTest/providers/Microsoft.ApiManagement/service/TestApiManagement1/providers/Microsoft.Insights/metrics/UnauthorizedRequests\",\"type\":\"Microsoft.Insights/metrics\",\"name\":{\"value\":\"UnauthorizedRequests\",\"localizedValue\":\"Unauthorized Gateway Requests\"},\"unit\":\"Count\",\"timeseries\":[{\"metadatavalues\":[],\"data\":[{\"timeStamp\":\"2018-08-28T09:02:00Z\",\"total\":0.0},{\"timeStamp\":\"2018-08-28T09:03:00Z\",\"total\":0.0}]}]}],\"namespace\":\"Microsoft.ApiManagement/service\",\"resourceregion\":\"northeurope\"}";
        String apiResponseWithQualifier = "{\"cost\":0,\"timespan\":\"2018-08-28T09:04:38Z/2018-08-28T09:06:38Z\",\"interval\":\"PT1M\",\"value\":[{\"id\":\"/subscriptions/1a2b3c4b-e5f6-g7h8-a123-1a23bcde456f/resourceGroups/AppDTest/providers/Microsoft.ApiManagement/service/TestApiManagement2/providers/Microsoft.Insights/metrics/UnauthorizedRequests\",\"type\":\"Microsoft.Insights/metrics\",\"name\":{\"value\":\"UnauthorizedRequests\",\"localizedValue\":\"Unauthorized Gateway Requests\"},\"unit\":\"Count\",\"timeseries\":[{\"metadatavalues\":[],\"data\":[{\"timeStamp\":\"2018-08-28T09:04:00Z\",\"total\":0.0},{\"timeStamp\":\"2018-08-28T09:05:00Z\",\"total\":0.0}]}]}],\"namespace\":\"Microsoft.ApiManagement/service\",\"resourceregion\":\"northeurope\"}";
        String apiResponseWithDimensions = "{\"cost\":0,\"timespan\":\"2018-08-28T11:53:19Z/2018-08-28T11:55:19Z\",\"interval\":\"PT1M\",\"value\":[{\"id\":\"/subscriptions/1a2b3c4b-e5f6-g7h8-a123-1a23bcde456f/resourceGroups/AppDTest/providers/Microsoft.EventHub/namespaces/TestHub1/providers/Microsoft.Insights/metrics/ThrottledRequests\",\"type\":\"Microsoft.Insights/metrics\",\"name\":{\"value\":\"ThrottledRequests\",\"localizedValue\":\"Throttled Requests. (Preview)\"},\"unit\":\"Count\",\"timeseries\":[{\"metadatavalues\":[{\"name\":{\"value\":\"entityname\",\"localizedValue\":\"entityname\"},\"value\":\"cooltopica\"}],\"data\":[{\"timeStamp\":\"2018-08-28T11:53:00Z\",\"total\":0.0},{\"timeStamp\":\"2018-08-28T11:54:00Z\",\"total\":0.0}]}]}],\"namespace\":\"Microsoft.EventHub/namespaces\",\"resourceregion\":\"northeurope\"}";

        String expectedOutputWithMetrics = "[AVERAGE/SUM/COLLECTIVE] [" + metricPrefix + subscriptionName + "|AppDTest|service|TestApiManagement1|SuccessfulRequests]=[0.0]]"
                + "[AVERAGE/SUM/COLLECTIVE] [" + metricPrefix + subscriptionName + "|AppDTest|service|TestApiManagement1|FailedRequests]=[0.0]]"
                + "[AVERAGE/SUM/COLLECTIVE] [" + metricPrefix + subscriptionName + "|AppDTest|service|TestApiManagement1|UnauthorizedRequests]=[0.0]]";

        String expectedOutputWithQualifier = "[AVERAGE/AVERAGE/COLLECTIVE] [" + metricPrefix + subscriptionName + "|AppDTest|service|TestApiManagement2|UnauthorizedRequests]=[0.0]]";

        String expectedOutputWithDimensions = "[AVERAGE/SUM/COLLECTIVE] [" + metricPrefix + subscriptionName + "|AppDTest|namespaces|TestHub1|coolTopicA|Throttled Requests]=[0.0]]";

        return Arrays.asList(new Object[][] {
            {resourcesWithMetrics, metricDefinitionsWithMetrics, apiResponseWithMetrics, "SuccessfulRequests,FailedRequests,UnauthorizedRequests", null, expectedOutputWithMetrics},
            {resourcesWithQualifier, metricDefinitionsWithQualifier, apiResponseWithQualifier, "UnauthorizedRequests", null, expectedOutputWithQualifier},
            {resourcesWithDimensions, metricDefinitionsWithDimensions, apiResponseWithDimensions, "ThrottledRequests", "EntityName eq 'cooltopica'", expectedOutputWithDimensions}
        });
    }

    @Test
    @SuppressWarnings({"ConstantConditions", "unchecked"})
    public void testAzureMonitorTask() {

        AzureAPIWrapper azure = mockAzureAPIWrapper(subscription, metricWriteHelper);
        mockExtensionOutput();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        AzureMonitorTask task = new AzureMonitorTask(monitorContextConfiguration, metricWriteHelper, subscription, countDownLatch, azure);
        monitorContextConfiguration.getContext().getExecutorService().execute("Azure Monitor", task);
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private AzureAPIWrapper mockAzureAPIWrapper(Map<String, String> subscription, MetricWriteHelper metricWriteHelper) {
        AzureAPIWrapper azure = mock(AzureAPIWrapper.class);
        
        when(azure.getResources()).thenReturn(resources);     
        when(azure.getMetricDefinitions(resources.get(0).getId())).thenReturn(metricDefinitions);

        JsonNode metrics = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            metrics = objectMapper.readTree(metricsApiResponse);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            when(azure.getMetrics(metricDefinitions.get(0), metricNames)).thenReturn(metrics);
            when(azure.getMetrics(metricDefinitions.get(0), metricNames, filter)).thenReturn(metrics);
        } catch (MalformedURLException | UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return azure;
    }

    public void mockExtensionOutput() {
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                List<Metric> metrics = (List<Metric>) invocation.getArguments()[0];
                Object mock = invocation.getMock();
                String realOutput = "";
                String loggerOutput = "\n";
                for (Object metric : metrics) {
                    realOutput += metric.toString();
                    loggerOutput += "Printing metric " + metric.toString() + "\n";
                }
                assertEquals(expectedOutput, realOutput);
                logger.debug(loggerOutput);

                return null;
              }
           })
        .when(metricWriteHelper)
        .transformAndPrintMetrics(Mockito.any(List.class));
    }
}
