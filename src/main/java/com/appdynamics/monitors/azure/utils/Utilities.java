/*
 * Copyright 2017. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.monitors.azure.utils;

import com.appdynamics.extensions.crypto.CryptoUtil;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.MalformedURLException;
import java.net.URL;

public class Utilities {
    private static final Logger logger = LoggerFactory.getLogger(Utilities.class);

    public static String getDecryptedKey(String encryptedKey, String encryptionKey){
        java.util.Map<String, String> cryptoMap = Maps.newHashMap();
        cryptoMap.put("password-encrypted", encryptedKey);
        cryptoMap.put("encryption-key", encryptionKey);
        return CryptoUtil.getPassword(cryptoMap);
    }

    public static URL getUrl(String input){
        URL url = null;
        try {
            url = new URL(input);
        } catch (MalformedURLException e) {
            logger.error("Error forming our from String {}", input, e);
        }
        return url;
    }

// --Commented out by Inspection START (7/4/18 6:16 PM):
//    static String prettifyJson(JsonNode json) {
//        ObjectMapper mapper = new ObjectMapper();
//        try {
//            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
//        } catch (JsonProcessingException e) {
//            logger.error("Can not process JSON {}", json.asText(), e);
//        }
//        return null;
//    }
// --Commented out by Inspection STOP (7/4/18 6:16 PM)
}
