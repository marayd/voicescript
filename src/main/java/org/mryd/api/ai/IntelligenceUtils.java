/*
 * IntelligenceUtils.java
 *
 * AI utilities for generating assistant responses using a language model API.
 *
 * Created by mryd — https://mryd.org/
 * GitHub: https://github.com/marayd
 *
 * This utility provides methods to interact with large language model APIs
 * by constructing appropriate requests, managing context messages, and parsing
 * the assistant's generated responses.
 *
 * Author: mryd
 */

package org.mryd.api.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

@Slf4j
public class IntelligenceUtils {

    private static final int MAX_TOKENS = 300;

    /**
     * Generates content with specified API URL and API key.
     */
    public static String generateContent(String systemInstruction, String prompt, String apiUrl, String apiKey, String model) throws IOException {
        HttpURLConnection connection = createConnection(apiUrl, apiKey);
        JsonObject requestBody = buildRequestBody(systemInstruction, prompt, model);

        sendRequest(connection, requestBody);
        return readResponse(connection);
    }

    /**
     * Creates an HTTP connection for the API request.
     */
    private static HttpURLConnection createConnection(String apiUrl, String apiKey) throws IOException {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URI(apiUrl).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setDoOutput(true);
            return connection;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid API URL: " + apiUrl, e);
        }
    }

    /**
     * Builds the JSON request body to be sent to the API.
     */
    private static JsonObject buildRequestBody(String systemInstruction, String prompt, String model) {
        JsonArray messages = new JsonArray();

        // Add system message
        messages.add(createMessage("system", systemInstruction));

        // Add current prompt
        messages.add(createMessage("user", prompt));

        // Build full request object
        JsonObject request = new JsonObject();
        request.addProperty("model", model);
        request.addProperty("stream", false);
        request.addProperty("max_tokens", MAX_TOKENS);
        request.add("messages", messages);

        return request;
    }

    /**
     * Sends the HTTP request with the given JSON body.
     */
    private static void sendRequest(HttpURLConnection connection, JsonObject requestBody) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(new Gson().toJson(requestBody));
            writer.flush();
        }
    }

    /**
     * Reads the response from the API.
     */
    private static String readResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        InputStream inputStream = (responseCode == HttpURLConnection.HTTP_OK)
                ? connection.getInputStream()
                : connection.getErrorStream();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder responseBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }

            return extractContent(responseBuilder.toString());
        }
    }

    /**
     * Extracts the assistant's content from the JSON response.
     */
    private static String extractContent(String jsonResponse) {
        JsonObject responseObj = JsonParser.parseString(jsonResponse).getAsJsonObject();
        JsonArray choices = responseObj.getAsJsonArray("choices");

        if (choices != null && !choices.isEmpty()) {
            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            if (message != null && message.has("content")) {
                return message.get("content").getAsString();
            }
        }

        return "Error: empty or malformed response";
    }

    /**
     * Creates a message object with the given role and content.
     */
    private static JsonObject createMessage(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        return message;
    }
}
