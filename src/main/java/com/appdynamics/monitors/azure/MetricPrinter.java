package com.appdynamics.monitors.azure;

import com.appdynamics.extensions.util.MetricWriteHelper;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MetricPrinter {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MetricPrinter.class);
    private final MetricWriteHelper metricWriter;

    MetricPrinter(MetricWriteHelper metricWriter){
        this.metricWriter = metricWriter;
    }

    void reportMetric(String metricName, BigDecimal metricValue) {
        if(metricValue == null){
            return;
        }
        printMetric(metricName,
                metricValue,
                MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
    }

    private void printMetric(String metricPath, BigDecimal metricValue, String aggType, String timeRollupType, String clusterRollupType) {

        try{
            String metricValStr = toBigIntString(metricValue);
            if(metricValStr != null) {
                metricWriter.printMetric(metricPath,metricValStr,aggType,timeRollupType,clusterRollupType);
                logger.debug("Sending [{}|{}|{}] metric= {},value={}", aggType, timeRollupType, clusterRollupType, metricPath, metricValStr);
            }
        }
        catch (Exception e){
            logger.error("Error reporting metric {} with value {}",metricPath,metricValue,e);
        }
    }

    private String toBigIntString(BigDecimal metricValue) {
        return metricValue.setScale(0, RoundingMode.HALF_UP).toBigInteger().toString();
    }
}
