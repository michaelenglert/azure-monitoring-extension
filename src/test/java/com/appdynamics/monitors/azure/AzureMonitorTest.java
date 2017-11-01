package com.appdynamics.monitors.azure;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.monitors.azure.config.Globals;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import java.math.BigDecimal;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.assertTrue;

public class AzureMonitorTest {

    @Test
    public void testAzureMonitor(){
        Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put("config-file", "src/test/resources/conf/integration-test-config.yml");
        try {
            testAzureMonitorRun(taskArgs);
        } catch (TaskExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
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
        } catch (InterruptedException e) {
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

    public void testAzureMonitorRun(Map<String, String> taskArgs) throws TaskExecutionException, InterruptedException {
        TaskOutput result = new AzureMonitor().execute(taskArgs, null);
        assertTrue(result.getStatusMessage().contains("Metric Upload Complete"));
    }

    public void testAzureMonitorTaskRun(String configYml) throws Exception {
        MetricWriteHelper writer = Mockito.mock(MetricWriteHelper.class);
        Runnable runner = Mockito.mock(Runnable.class);
        MonitorConfiguration conf = new MonitorConfiguration(Globals.defaultMetricPrefix, runner, writer);
        conf.setConfigYml(configYml);
        Mockito.doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                System.out.println(args[0] + "=" + args[1]);
                return null;
            }
        }).when(writer).printMetric(Mockito.anyString(), Mockito.any(BigDecimal.class), Mockito.anyString());
        conf.setMetricWriter(writer);

        AuthenticationResult azureAuth = AzureAuth.getAzureAuth(
                (String) conf.getConfigYml().get(Globals.clientId),
                Utilities.getClientKey(conf.getConfigYml()),
                (String) conf.getConfigYml().get(Globals.tenantId));

        //noinspection unchecked
        List<Map> filters = (List<Map>) conf.getConfigYml().get(Globals.azureApiFilter);
        String filterUrl = Utilities.getFilters(filters);
        URL url = new URL(Globals.azureEndpoint + Globals.azureApiSubscriptions + conf.getConfigYml().get(Globals.subscriptionId) + Globals.azureApiResources +
                "?" + Globals.azureApiVersion + "=" + conf.getConfigYml().get(Globals.azureApiVersion) +
                filterUrl);
        ArrayNode elements = (ArrayNode) AzureRestOperation.doGet(azureAuth,url).get("value");
        for(JsonNode node:elements){
            AzureMonitorTask task = new AzureMonitorTask(conf, node, azureAuth);
            conf.getExecutorService().execute(task);
        }
        conf.getExecutorService().awaitTermination(2, TimeUnit.SECONDS);
    }

    @Test(expected = TaskExecutionException.class)
    public void testAzureMonitorTaskExcecutionException() throws Exception {
        new AzureMonitor().execute(null, null);
    }
}
