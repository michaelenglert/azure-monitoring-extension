package com.appdynamics.monitors.azure;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
import com.appdynamics.extensions.conf.MonitorConfiguration.ConfItem;
import com.appdynamics.monitors.azure.config.Globals;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("WeakerAccess")
public class AzureMonitor extends AManagedMonitor {
    private static final Logger logger = LoggerFactory.getLogger(AzureMonitor.class);
    private MonitorConfiguration configuration;

    public AzureMonitor() { logger.info(String.format("Using Azure Monitor Version [%s]", getImplementationVersion())); }

    private void initialize(Map<String, String> argsMap) {
        if (configuration == null) {
            MetricWriteHelper metricWriteHelper = MetricWriteHelperFactory.create(this);
            MonitorConfiguration conf = new MonitorConfiguration(Globals.defaultMetricPrefix,
                    new TaskRunnable(), metricWriteHelper);
            final String configFilePath = argsMap.get(Globals.configFile);
            conf.setConfigYml(configFilePath);
            conf.setMetricWriter(MetricWriteHelperFactory.create(this));
            conf.checkIfInitialized(ConfItem.CONFIG_YML, ConfItem.EXECUTOR_SERVICE, ConfItem.METRIC_PREFIX, ConfItem.METRIC_WRITE_HELPER);
            this.configuration = conf;
        }
    }

    public TaskOutput execute(Map<String, String> map, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        try{
            if(map != null){
                if (logger.isDebugEnabled()) {logger.debug("The raw arguments are {}", map);}
                initialize(map);
                configuration.executeTask();
                return new TaskOutput("Azure Monitor Metric Upload Complete");
            }
        }
        catch(Exception e) {
            logger.error("Failed to execute the Azure monitoring task", e);
        }
        throw new TaskExecutionException("Azure monitoring task completed with failures.");

    }

    private class TaskRunnable implements Runnable {
        public void run () {
            Map<String, ?> config = configuration.getConfigYml();
            if (config != null ) {
                try {
                    AuthenticationResult azureAuth = AzureAuth.getAzureAuth(
                            (String) config.get(Globals.clientId),
                            Utilities.getClientKey(config),
                            (String) config.get(Globals.tenantId));
                    @SuppressWarnings("unchecked") List<Map> filters = (List<Map>) config.get(Globals.azureApiFilter);
                    String filterUrl = Utilities.getFilters(filters);
                    URL resourcesUrl = new URL(Globals.azureEndpoint + Globals.azureApiSubscriptions + config.get(Globals.subscriptionId) + Globals.azureApiResources +
                            "?" + Globals.azureApiVersion + "=" + config.get(Globals.azureApiVersion) +
                            filterUrl);
                    JsonNode resourcesResponse = AzureRestOperation.doGet(azureAuth,resourcesUrl);
                    if (logger.isDebugEnabled()) { logger.debug("Get Resources REST API Request: " + resourcesUrl.toString());logger.debug("Get Resources Response JSON: " + Utilities.prettifyJson(resourcesResponse)); }
                    ArrayNode resourceElements = (ArrayNode) resourcesResponse.get("value");
                    for(JsonNode resourceNode:resourceElements){
                        URL metricDefinitions = new URL(Globals.azureEndpoint + resourceNode.get("id").asText() + Globals.azureApiMetricDefinitions + "?" + Globals.azureApiVersion + "=" + config.get(Globals.azureMonitorApiVersion));
                        JsonNode metricDefinitionResponse = AzureRestOperation.doGet(azureAuth,metricDefinitions);
                        if (logger.isDebugEnabled()) { logger.debug("Get Metric Definitions REST API Request: " + metricDefinitions.toString());logger.debug("Get Metric Definitions Response JSON: " + Utilities.prettifyJson(metricDefinitionResponse)); }
                        ArrayNode metricDefinitionElements = (ArrayNode) metricDefinitionResponse.get("value");
                        for(JsonNode metricDefinitionNode:metricDefinitionElements){
                            AzureMonitorTask task = new AzureMonitorTask(configuration, resourceNode, azureAuth, metricDefinitionNode.get("name").get("value").asText());
                            configuration.getExecutorService().execute(task);
                        }
                    }
                    logger.info("Finished gathering Metrics");
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
            else { logger.error("The config.yml is not loaded due to previous errors.The task will not run"); }
        }
    }

    private static String getImplementationVersion() { return AzureMonitor.class.getPackage().getImplementationTitle(); }
}
