# AppDynamics Azure - Monitoring Extension

This extension works only with the standalone machine agent.

## Use Case



## Prerequisites

 * This extension requires the Java Machine Agent.

## Installation

Either [Download the Extension from the AppDynamics Marketplace](https://www.appdynamics.com/community/exchange/azure-monitoring-extension/) or Build from Source.

1. Deploy the `AzureMonitor-<VERSION>.zip` file into the `<machine agent home>/monitors` directory.

  `> unzip AzureMonitor-<VERSION>.zip -d <machine agent home>/monitors/`

2. Set up config.yml. At minimum this is:

  ```
  #Client ID obtained from the Azure Portal
  clientId: ""

  #Client Key for the upper ID obtained from the Azure Portal
  clientKey: ""

  #Tenant ID obtained from the Azure Portal
  tenantId: ""

  #Subscription ID obtained from the Azure Portal
  subscriptionId: ""
  ```
  Details for the Setup can be found in the [Azure - Resource Manager - Howto - Control Access - Create Service Principal - Azure Portal](https://docs.microsoft.com/en-us/azure/azure-resource-manager/resource-group-create-service-principal-portal)

3. Restart the Machine Agent.

## Build from Source

1. Clone this repository
2. Run `mvn -DskipTests clean install`
3. The `AzureMonitor-<VERSION>.zip` file can be found in the `target` directory

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


## Metrics

Metrics available for Azure are maintained with the [Azure Monitor Documentation - Reference - Metrics Supported](https://docs.microsoft.com/en-us/azure/monitoring-and-diagnostics/monitoring-supported-metrics)

## Contributing

Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/michaelenglert/azure-monitoring-extension).

## Community

## Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:help@appdynamics.com).
