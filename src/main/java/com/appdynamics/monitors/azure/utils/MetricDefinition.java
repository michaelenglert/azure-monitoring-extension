package com.appdynamics.monitors.azure.utils;

public class MetricDefinition {
    private String id;
    private String name;
    private String primaryAggregationType;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getPrimaryAggregationType() {
        return primaryAggregationType;
    }
    public void setPrimaryAggregationType(String primaryAggregationType) {
        this.primaryAggregationType = primaryAggregationType;
    }
}
