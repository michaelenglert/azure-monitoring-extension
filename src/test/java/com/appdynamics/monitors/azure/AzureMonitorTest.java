package com.appdynamics.monitors.azure;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.monitors.azure.config.AuthenticationResults;
import com.appdynamics.monitors.azure.config.Globals;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AzureMonitorTest {
    private static final Logger logger = LoggerFactory.getLogger(AzureMonitorTest.class);

    @Test
    public void testAzureMonitor(){
        Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put("config-file", "src/test/resources/conf/integration-test-config.yml");
        try {
            testAzureMonitorRun(taskArgs);
        } catch (TaskExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAzureMonitorWithEncryption(){
        Map<String, String> taskArgs = new HashMap<String, String>();
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

    @Test
    public void testExtractMetrics() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(new FileReader("src/test/resources/json/metric.json"));

        JsonNode jsonValue = json.get("value");
        for (JsonNode currentValueNode:jsonValue){
            String metricNameValue = currentValueNode.get("name").get("value").asText();
            String metricUnit = currentValueNode.get("unit").asText();
            String metricType = null;
            BigDecimal metricValue = null;
            if(currentValueNode.get("timeseries").has(0)){
                JsonNode jsonData = currentValueNode.get("timeseries").get(0).get("data");
                for (JsonNode currentDataNode:jsonData){
                    if (currentDataNode.has("average")){ metricType = "average"; metricValue = currentDataNode.get("average").decimalValue(); }
                    else if (currentDataNode.has("total")){ metricType = "total"; metricValue = currentDataNode.get("total").decimalValue(); }
                    else if (currentDataNode.has("last")){ metricType = "last"; metricValue = currentDataNode.get("last").decimalValue(); }
                    else if (currentDataNode.has("maximum")){ metricType = "maximum"; metricValue = currentDataNode.get("maximum").decimalValue(); }
                }
            }
            assertNotNull(metricValue);
            assertNotNull(metricType);
            assertNotNull(metricUnit);
            assertNotNull(metricNameValue);
        }
    }


    private void testAzureMonitorRun(Map<String, String> taskArgs) throws TaskExecutionException {
        TaskOutput result = new AzureMonitor().execute(taskArgs, null);
        assertTrue(result.getStatusMessage().contains("Metric Upload Complete"));
    }

    @SuppressWarnings("ConstantConditions")
    private void testAzureMonitorTaskRun(String configYml) throws Exception {
        MetricWriteHelper writer = Mockito.mock(MetricWriteHelper.class);
        Runnable runner = Mockito.mock(Runnable.class);
        MonitorConfiguration conf = new MonitorConfiguration(Globals.defaultMetricPrefix, runner, writer);
        conf.setConfigYml(configYml);
        Mockito.doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocationOnMock) { Object[] args = invocationOnMock.getArguments();
                 System.out.println(args[0] + "=" + args[1]);
                 return null;
             }
        }).when(writer).printMetric(Mockito.anyString(), Mockito.any(BigDecimal.class), Mockito.anyString());
        conf.setMetricWriter(writer);

        AzureAuth.getAzureAuth(conf.getConfigYml());
        JsonNode filtersJson = Utilities.getFiltersJson((ArrayList) conf.getConfigYml().get(Globals.azureApiFilter));
        String filterUrl = Utilities.getFilterUrl(filtersJson);
        Map<String,String> resourceFilter = Utilities.getResourceFilter(filtersJson);
        URL url = Utilities.getUrl(Globals.azureEndpoint + Globals.azureApiSubscriptions + conf.getConfigYml().get(Globals.subscriptionId) + Globals.azureApiResources +
                "?" + Globals.azureApiVersion + "=" + conf.getConfigYml().get(Globals.azureApiVersion) +
                filterUrl);
        ArrayNode resourceElements = (ArrayNode) AzureRestOperation.doGet(AuthenticationResults.azureMonitorAuth,url).get("value");
        Utilities.prettifyJson(resourceElements);
        for(JsonNode resourceNode:resourceElements){
            URL metricDefinitions = Utilities.getUrl(Globals.azureEndpoint + resourceNode.get("id").asText() + Globals.azureApiMetricDefinitions + "?" + Globals.azureApiVersion + "=" + conf.getConfigYml().get(Globals.azureMonitorApiVersion));
            JsonNode metricDefinitionResponse = AzureRestOperation.doGet(AuthenticationResults.azureMonitorAuth,metricDefinitions);
            assert metricDefinitionResponse != null;
            ArrayNode metricDefinitionElements = (ArrayNode) metricDefinitionResponse.get("value");
            for(JsonNode metricDefinitionNode:metricDefinitionElements){
                if (metricDefinitionNode.get("isDimensionRequired").asText().equals("true")){
                    logger.info("Dimensions are currently not supported. Skipping "
                            + metricDefinitionNode.get("id").asText());
                }
                else if (Utilities.checkResourceFilter(metricDefinitionNode,resourceFilter)){
                    logger.info("Ignoring Metric " +
                            metricDefinitionNode.get("name").get("value").asText() +
                            " for Resource " + metricDefinitionNode.get("resourceId"));
                }
                else {
                    AzureMonitorTask monitorTask = new AzureMonitorTask(
                            conf,
                            resourceNode,
                            AuthenticationResults.azureMonitorAuth,
                            metricDefinitionNode.get("name").get("value").asText());
                    conf.getExecutorService().execute(monitorTask);
                }
            }
        }
        conf.getExecutorService().awaitTermination(2, TimeUnit.SECONDS);
    }

    @Test(expected = TaskExecutionException.class)
    public void testAzureMonitorTaskExcecutionException() throws Exception {
         new AzureMonitor().execute(null, null);
    }
}
