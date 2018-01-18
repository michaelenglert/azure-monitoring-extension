/*
 * Copyright 2017. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.monitors.azure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.aad.adal4j.AuthenticationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

class AzureRestOperation {
    private static final Logger logger = LoggerFactory.getLogger(AzureRestOperation.class);

    static JsonNode doGet(AuthenticationResult azureAuth, URL url) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String response = "";
            HttpURLConnection conn;
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + azureAuth.getAccessToken());
            conn.setRequestProperty("Content-Type", "application/json");
            BufferedReader br = new BufferedReader(new InputStreamReader(
                (conn.getInputStream())));
            //noinspection StatementWithEmptyBody
            for (String line; (line = br.readLine()) != null; response += line);
            conn.disconnect();
            return objectMapper.readTree(response);
        } catch (IOException e) {
            logger.error("Error while processing GET on URL {}", url, e);
            return null;
        }
    }

    static JsonNode doPost(URL url, String body) {
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            OutputStream output = conn.getOutputStream();
            output.write(body.getBytes("UTF-8"));
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));
            String response = "";
            //noinspection StatementWithEmptyBody
            for (String line; (line = br.readLine()) != null; response += line);
            conn.disconnect();
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readTree(response);
        } catch (IOException e) {
            logger.error("Error while processing POST on URL {} with Body {}", url, body, e);
            return null;
        }
    }
}
