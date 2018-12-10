package com.appdynamics.monitors.azure.utils;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.appdynamics.monitors.azure.AzureAuth;
import com.appdynamics.monitors.azure.AzureRestOperation;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.monitor.MetricDefinition;
import com.microsoft.azure.management.resources.GenericResource;

public class AzureAPIWrapper {
    private Azure azure;
    private AtomicInteger azureResourcesCallCount = new AtomicInteger(0);
    private AtomicInteger azureMetricDefinitionsCallCount = new AtomicInteger(0);
//    private AtomicInteger azureMetricsCallCount = new AtomicInteger(0);
    private Map<String, ?> subscription;

    public AzureAPIWrapper(Map<String, ?> subscription) {
        this.subscription = subscription;
    }

    public void authorize() {
        AzureAuth.authorizeAzure(subscription);
        azure = AzureAuth.getAzureMonitorAuth().withSubscription(subscription.get("subscriptionId").toString());
    }

    public List<Resource> getResources() {
        azureResourcesCallCount.incrementAndGet();
        PagedList<GenericResource> resources = retrieveResources();
        List<Resource> list = Lists.newArrayList();;

        for(GenericResource resource : resources) {
            Resource wrappedResource = new Resource();
            wrappedResource.setName(resource.name());
            wrappedResource.setType(resource.type());
            wrappedResource.setResourceGroupName(resource.resourceGroupName());
            wrappedResource.setResourceType(resource.resourceType());
            wrappedResource.setId(resource.id());
            list.add(wrappedResource);
        }

        return list;
    }

    private PagedList<GenericResource> retrieveResources() {
        // TODO Auto-generated method stub
        return azure.genericResources().list();
    }

    public List<com.appdynamics.monitors.azure.utils.MetricDefinition> getMetricDefinitions(String resourceId) {
        azureMetricDefinitionsCallCount.incrementAndGet();
        List<MetricDefinition> resourceMetrics = azure.metricDefinitions().listByResource(resourceId);

        List<com.appdynamics.monitors.azure.utils.MetricDefinition> list = Lists.newArrayList();;

        for(MetricDefinition resourceMetric : resourceMetrics) {
            com.appdynamics.monitors.azure.utils.MetricDefinition wrappedMetric = new com.appdynamics.monitors.azure.utils.MetricDefinition();
            wrappedMetric.setName(resourceMetric.name().value().toString());
            wrappedMetric.setPrimaryAggregationType(resourceMetric.primaryAggregationType().toString());
            wrappedMetric.setId(resourceMetric.id());
            list.add(wrappedMetric);
        }

        return list;
    }

    public JsonNode getMetrics(com.appdynamics.monitors.azure.utils.MetricDefinition metricDefinition, String metricNames) throws MalformedURLException, UnsupportedEncodingException {
        return getMetrics(metricDefinition, metricNames, null);
    }

    public JsonNode getMetrics(com.appdynamics.monitors.azure.utils.MetricDefinition metricDefinition, String metricNames, String filter) throws MalformedURLException, UnsupportedEncodingException {
        DateTime recordDateTime = DateTime.now(DateTimeZone.UTC);
        Integer ordinalIndexOfLastSlash = StringUtils.ordinalIndexOf(metricDefinition.getId(), "/", Constants.AZURE_METRICS_API_ENDPOINT_LAST_SLASH_POS);
        if (ordinalIndexOfLastSlash < 0) {
            throw new MalformedURLException("Invalid metrics API endpoint");
        }
        String apiEndpointBase = metricDefinition.getId().substring(0,ordinalIndexOfLastSlash);
        String url = Constants.AZURE_MANAGEMENT_URL
                + apiEndpointBase
                + "/metrics?timespan=" + recordDateTime.minusMinutes(2).toDateTimeISO() + "/" + recordDateTime.toDateTimeISO()
                + "&metricnames=" + metricNames
                + "&api-version=" + subscription.get("api-version");
        if (filter != null) {
            url += "&$filter=" + URLEncoder.encode(filter, "UTF-8");
        }
        URL apiEndpointFull = new URL(url);
//        azureMetricsCallCount.incrementAndGet();
        JsonNode apiResponse = AzureRestOperation.doGet(apiEndpointFull);

        return apiResponse;
    }
}
