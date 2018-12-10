package com.appdynamics.monitors.azure.utils;

public class Resource {

    private String name;
    private String type;
    private String resourceGroupName;
    private Object resourceType;
    private String id;

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public Object getResourceType() {
        return resourceType;
    }

    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public void setId(String id) {
        this.id = id;
    }

}
