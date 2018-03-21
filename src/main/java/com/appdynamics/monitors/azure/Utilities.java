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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class Utilities {
    private static final Logger logger = LoggerFactory.getLogger(Utilities.class);

    static String getFilters(List<Map> filters) {
        StringBuilder filterUrl = null;
        if (filters != null && !filters.isEmpty()) {
            filterUrl = new StringBuilder("&$" + Globals.azureApiFilter + "=");
            Iterator<Map> iter = filters.iterator();
            while (iter.hasNext()) {
                Map filter = iter.next();
                try {
                    filterUrl.append(URLEncoder.encode(Globals.filterBy +
                            Globals.filterComOp + "'" +
                            filter.get(Globals.filterBy) +
                            "'", Globals.urlEncoding));
                    if ( iter.hasNext()) {
                        filterUrl.append(URLEncoder.encode(Globals.filterLogOp, Globals.urlEncoding));
                    }
                }
                catch (UnsupportedEncodingException e) {
                    logger.error("Can not process filters {}", filters.toString(), e);
                }
            }
        }
        return filterUrl != null ? filterUrl.toString() : null;
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
