/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.monitors.azure;

import com.appdynamics.monitors.azure.utils.Constants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.*;
import java.util.HashMap;
import java.util.Map;

public class AzureRestOperation {
    private static final Logger logger = LoggerFactory.getLogger(AzureRestOperation.class);

    public static JsonNode doGet(URL url) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + Constants.accessToken);
        return doGet(url, headers);
    }

    public static JsonNode doGet(URL url, Map<String,String> headers) {
        try {
            logger.debug("--> GET " + url);
            ObjectMapper objectMapper = new ObjectMapper();
            String response = "";
            HttpURLConnection conn;
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            for (Map.Entry <String, String> header : headers.entrySet()) {
                conn.setRequestProperty(header.getKey(), header.getValue());
            }
            conn.setRequestProperty("Content-Type", "application/json");
            BufferedReader br = new BufferedReader(new InputStreamReader(
                (conn.getInputStream())));
            //noinspection StatementWithEmptyBody
            for (String line; (line = br.readLine()) != null; response += line);
            conn.disconnect();
            if (logger.isDebugEnabled()) {
                logger.debug("API response: " + response);
            }
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

    static JsonNode doSecurePost(URL url, String body, String cert, String passphrase) {
        JsonNode responseJson = null;
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            FileInputStream fis = new FileInputStream(cert);
            ks.load(fis, passphrase.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, passphrase.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); // PKIX
            tmf.init(ks);
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            if (conn instanceof HttpsURLConnection) {
                ((HttpsURLConnection)conn)
                        .setSSLSocketFactory(sc.getSocketFactory());
                ((HttpsURLConnection)conn)
                        .setHostnameVerifier(allHostsValid);
            }
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
            responseJson = objectMapper.readTree(response);
        } catch (Exception e) {
            logger.error("Error while processing POST on URL {} with Body {}", url, body, e);
        }
        return responseJson;
    }
}
