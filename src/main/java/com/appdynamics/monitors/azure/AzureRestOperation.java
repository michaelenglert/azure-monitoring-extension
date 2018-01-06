package com.appdynamics.monitors.azure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.aad.adal4j.AuthenticationResult;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

class AzureRestOperation {
    static JsonNode doGet(AuthenticationResult azureAuth, URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + azureAuth.getAccessToken());
        conn.setRequestProperty("Content-Type", "application/json");
        BufferedReader br = new BufferedReader(new InputStreamReader(
                (conn.getInputStream())));
        String response = "";
        //noinspection StatementWithEmptyBody
        for (String line; (line = br.readLine()) != null; response += line);
        conn.disconnect();
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(response);
    }

    static JsonNode doPost(URL url, String body) throws IOException {
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
    }
}
