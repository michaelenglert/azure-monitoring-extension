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
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
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
                    //noinspection unchecked
                    List<Map> filters = (List<Map>) config.get(Globals.azureApiFilter);
                    String filterUrl = Utilities.getFilters(filters);
                    URL url = new URL(Globals.azureEndpoint + Globals.azureApiSubscriptions + config.get(Globals.subscriptionId) + Globals.azureApiResources +
                            "?" + Globals.azureApiVersion + "=" + config.get(Globals.azureApiVersion) +
                            filterUrl);
                    JsonNode response = AzureRestOperation.doGet(azureAuth,url);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Initial REST API Request: " + url.toString());
                        logger.debug("Initial Response JSON: " + AzureRestOperation.prettifyJson(response));
                    }
                    ArrayNode elements = (ArrayNode) response.get("value");
                    for(JsonNode node:elements){
                        AzureMonitorTask task = new AzureMonitorTask(configuration, node, azureAuth);
                        configuration.getExecutorService().execute(task);
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                logger.error("The config.yml is not loaded due to previous errors.The task will not run");
            }
        }
    }

    private static String getImplementationVersion() { return AzureMonitor.class.getPackage().getImplementationTitle(); }
}
