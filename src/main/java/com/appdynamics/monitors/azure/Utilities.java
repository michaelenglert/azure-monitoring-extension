/*
 * Copyright 2017. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.monitors.azure;

import com.appdynamics.extensions.crypto.CryptoUtil;
import com.appdynamics.monitors.azure.config.Globals;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

class Utilities {
    private static final Logger logger = LoggerFactory.getLogger(Utilities.class);

    static JsonNode getFiltersJson(ArrayList filters){
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonInString;
        JsonNode filtersJson = null;
        try {
            jsonInString = objectMapper.writeValueAsString(filters);
            filtersJson = objectMapper.readTree(jsonInString);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filtersJson;
    }

    static String getFilterUrl(JsonNode filtersJson){
        StringBuilder filterUrl = null;
        Iterator<JsonNode> iter = filtersJson.iterator();
        JsonNode currentValueNode;
        if (!filtersJson.isNull()) {
            filterUrl = new StringBuilder("&$" + Globals.azureApiFilter + "=");
            while (iter.hasNext()) {
                currentValueNode = iter.next();
                try {
                    filterUrl.append(URLEncoder.encode(Globals.filterBy +
                            Globals.filterComOp + "'" +
                            currentValueNode.get(Globals.filterBy).asText() +
                            "'", Globals.urlEncoding));
                    if (iter.hasNext()) {
                        filterUrl.append(URLEncoder.encode(Globals.filterLogOp, Globals.urlEncoding));
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

            }
        }
        return filterUrl != null ? filterUrl.toString() : null;
    }

    static Map<String,String> getResourceFilter(JsonNode filtersJson) {
        Map<String,String> resourceFilter = new HashMap<String, String>();
        for (JsonNode currentValueNode:filtersJson){
            if(currentValueNode.has("exclude")){
                if (!currentValueNode.get("exclude").asText().isEmpty()){
                    resourceFilter.put(currentValueNode.get(Globals.filterBy).asText(),currentValueNode.get("exclude").asText());
                }
            }
        }
        return resourceFilter;
    }

    static Boolean checkResourceFilter(JsonNode jsonNode, Map<String,String> resourceFilter){
        for(Map.Entry<String, String> entry : resourceFilter.entrySet()){
            if(jsonNode.get("resourceId").asText().contains(entry.getKey())){
                if (jsonNode.get("name").get("value").asText().matches(entry.getValue())){
                    return true;
                }
            }
        }
        return false;
    }

    static String getDecryptedKey(String encryptedKey, String encryptionKey){
        java.util.Map<String, String> cryptoMap = Maps.newHashMap();
        cryptoMap.put(Globals.passwordEncrypted, encryptedKey);
        cryptoMap.put(Globals.encryptionKey, encryptionKey);
        return CryptoUtil.getPassword(cryptoMap);
    }

    static URL getUrl(String input){
        URL url = null;
        try {
            url = new URL(input);
        } catch (MalformedURLException e) {
            logger.error("Error forming our from String {}", input, e);
        }
        return url;
    }

    static String prettifyJson(JsonNode json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (JsonProcessingException e) {
            logger.error("Can not process JSON {}", json.asText(), e);
        }
        return null;
    }
}
