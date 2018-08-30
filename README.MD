# AppDynamics Azure - Monitoring Extension

This extension works only with the standalone machine agent.

## Use Case

Monitor Metrics provided by the Azure Monitor API and let them report into the AppDynamics Metric Browser.

## Prerequisites

 * This extension requires the Java Machine Agent.

## Installation

Either [Download the Extension from the Github release](https://github.com/michaelenglert/azure-monitoring-extension/releases) or Build from Source.

1. Deploy the `AzureMonitor-<VERSION>.zip` file into the `<machine agent home>/monitors` directory.

  `> unzip AzureMonitor-<VERSION>.zip -d <machine agent home>/monitors/`

2. Set up `config.yml`. At minimum this is:
  ```
subscriptions:
  - subscriptionId: ""
    tenantId: ""
    clientId: ""
    clientKey: ""
  ```
  Details for the Setup can be found in the [Azure - Resource Manager - Howto - Control Access - Create Service Principal - Azure Portal](https://docs.microsoft.com/en-us/azure/azure-resource-manager/resource-group-create-service-principal-portal)

3. Restart the Machine Agent.

## Build from Source

1. Clone this repository
2. Run `mvn -DskipTests clean install`
3. The `AzureMonitor-<VERSION>.zip` file can be found in the `target` directory

## CLI support

It is possible to test the extension without the machine agent, and without sending real metrics to the controller by using the command line. Both the machine agent and the extension jar files must be present in the class path.

Usage: java -Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG -cp ./machine-agent-x.x.xx.jar:./target/azure-monitoring-extension.jar com.appdynamics.monitors.azure.AzureMonitor

NOTE: Depending on JRE version, you might need to add this parameter "--add-modules java.xml.bind"

## Directory Structure

<table><tbody>
<tr>
<th align = 'left'> Directory/File </th>
<th align = 'left'> Description </th>
</tr>
<tr>
<td class='confluenceTd'> src/main/resources/config </td>
<td class='confluenceTd'> Contains monitor.xml and config.yml</td>
</tr>
<tr>
<td class='confluenceTd'> src/main/java </td>
<td class='confluenceTd'> Contains source code for the Azure monitoring extension </td>
</tr>
<tr>
<td class='confluenceTd'> src/test/java </td>
<td class='confluenceTd'> Contains test code for the Azure monitoring extension </td>
</tr>
<tr>
<td class='confluenceTd'> target </td>
<td class='confluenceTd'> Only obtained when using maven. Run 'maven clean install' to get the distributable .zip file. </td>
</tr>
<tr>
<td class='confluenceTd'> pom.xml </td>
<td class='confluenceTd'> maven build script to package the project (required only if changing Java code) </td>
</tr>
</tbody>
</table>

## Authentication using MSI (Managed Service Identity)

Managed Service Identity is a feature of Azure Active Directory that simplifies the way that credentials are handled. More info here: https://docs.microsoft.com/en-us/azure/active-directory/managed-service-identity/overview

To make use of this feature on a subscription, set the parameter useMSI to true in the config.yml file.
  ```
subscriptions:
  - subscriptionId: "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
    useMSI: "true"
  ```

## Dimensions

In case you need more granularity for the metrics, it is possible to define filters to use Azure dimensions. For more info, check https://docs.microsoft.com/en-us/azure/monitoring-and-diagnostics/monitoring-rest-api-walkthrough#retrieve-metric-definitions-multi-dimensional-api

  ```
                metrics:
                  - metric: "ThrottledRequests"
                    filter: "EntityName eq 'myTopic'"
                    alias: "Throttled Requests"
                    subpath: "myTopic"
  ```

As a result, this would be the path of the metric that would be sent to the controller (note the use of the alias parameter to change the name of the metric in the path):

Custom Metrics|AzureMonitor|xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx|resourceGroup1|namespaces|TestEventHub1|myTopic|Throttled Requests

## Metrics

Metrics available for Azure are maintained within the [Azure Monitor Documentation - Reference - Metrics Supported](https://docs.microsoft.com/en-us/azure/monitoring-and-diagnostics/monitoring-supported-metrics)

## Metrics alias

The same way as with dimensions, it is possible to use the alias parameter to change the name of any simple metric in the path.

  ```
                metrics:
                  - metric: "ThrottledRequests"
                    alias: "Throttled Requests"
  ```
And this would be the path of the metric sent to the controller:

Custom Metrics|AzureMonitor|xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx|resourceGroup1|namespaces|TestEventHub1|Throttled Requests

## Metric time rollup

The time-rollup qualifier specifies how the Controller rolls up the values when it converts from one-minute granularity tables to 10-minute granularity and 60-minute granularity tables over time. For more info, check https://docs.appdynamics.com/display/PRO45/Build+a+Monitoring+Extension+Using+Java#BuildaMonitoringExtensionUsingJava-MetricProcessingQualifier

  ```
                  - metric: "UnauthorizedRequests"
                    timeRollUp: "METRIC_TIME_ROLLUP_TYPE_AVERAGE"
  ```

## Excluding Metrics for Resources

You can exclude certain Metrics by adding an include within a filter Resource Element. To do this the following configuration in `config.yml` is required:
  ```
  subscriptions:
    - subscriptionId: ""
      tenantId: ""
      clientId: ""
      clientKey: ""
      resourceGroups:             # You can Filter with regex on any of these Types.
        - resourceGroup: ".*"     # If you have multiple entries per Type be sure they do not overlap
          resourceTypes:          # --> This will produce duplicate Metrics!!!!
            - resourceType: ".*"
              resources:
                - resource: ".*"
                  metrics:
                  - metric: ".*"
  ```
A regular expression is needed. Any match will be included for that specific type.

## Integration with Azure Key Vault

If you do not want to store the Client Key from the Service Principal that is used to query Resources, Metric Definitions and Metrics you can obtain the Key from an Azure Key Vault Secret.
To achieve this the following configuration in `config.yml` is required:
  ```
  subscriptions:
    - subscriptionId: ""
      tenantId: ""
      clientId: ""
      keyvaultClientId: ""
      keyvaultClientKey: ""
      keyvaultClientSecretUrl: ""
  ```
The **Key Vault Client Key** can also be encrypted just as the normal **Client Key**.

## Service Fabric Authentication

Currently the Extension support Certificate based Authentication to gather the Health Status of your Service Fabric Clusters. To use this those properties in `config.yml` have to be used:

  ```
  serviceFabrics:
    - serviceFabric: "ServiceFabric"
      serviceFabricCert: 'src/test/resources/cert/integration-test-sf.pfx'
      serviceFabricPassphrase: ''
  ```

## Password Encryption Support

To avoid setting the clear text password in the config.yml. Please follow the process to encrypt the password and set the encrypted password and the key in the config.yml

* Download the util jar to encrypt the password from [here](https://github.com/Appdynamics/maven-repo/raw/master/releases/com/appdynamics/appd-exts-commons/2.0.4/appd-exts-commons-2.0.4.jar)
* Encrypt password from the commandline 
  * `java -cp appd-exts-commons-<VERSION>.jar com.appdynamics.extensions.crypto.Encryptor myEncryptionKey myPasswordToEnrypt`

## Contributing

Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/michaelenglert/azure-monitoring-extension).

## Troubleshooting

1. Verify Machine Agent Data: Please start the Machine Agent without the extension and make sure that it reports data. Verify that the machine agent status is UP and it is reporting Hardware Metrics.
2. config.yml: Validate the file [here](http://www.yamllint.com/)
3. Metric Limit: Please start the machine agent with the argument `-Dappdynamics.agent.maxMetrics=5000` if there is a metric limit reached error in the logs. If you don't see the expected metrics, this could be the cause.
4. Check Logs: There could be some obvious errors in the machine agent logs. Please take a look.
5. `The config cannot be null` error.
   This usually happenes when on a windows machine in monitor.xml you give config.yaml file path with linux file path separator */*. Use Windows file path separator *\* e.g. *monitors\Monitor\config.yaml*. For Windows, please specify
   the complete path
6. Collect Debug Logs: Edit the file, *<MachineAgent>/conf/logging/log4j.xml* and update the level of the appender *com.appdynamics* and *com.singularity* to debug. Let it run for 5-10 minutes and attach the logs to a support ticket.

## Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:help@appdynamics.com).
