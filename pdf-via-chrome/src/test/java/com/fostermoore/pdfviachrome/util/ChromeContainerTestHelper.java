package com.fostermoore.pdfviachrome.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utility class for Chrome container tests using Testcontainers.
 *
 * Provides helper methods for working with containerized Chrome instances
 * in integration tests.
 */
public class ChromeContainerTestHelper {

    private ChromeContainerTestHelper() {
        // Utility class - prevent instantiation
    }

    /**
     * Gets the WebSocket debugger URL from Chrome's debugging endpoint.
     *
     * Makes an HTTP GET request to Chrome's /json/version endpoint and parses
     * the response to extract the webSocketDebuggerUrl field. The URL is then
     * modified to replace localhost/127.0.0.1 with the actual container host.
     *
     * @param host the host where Chrome is running (container host)
     * @param port the port where Chrome's debugging endpoint is exposed
     * @return the WebSocket debugger URL for connecting to Chrome via CDP
     * @throws IOException if the request fails or the response cannot be parsed
     */
    public static String getWebSocketDebuggerUrl(String host, int port) throws IOException {
        String jsonUrl = String.format("http://%s:%d/json/version", host, port);

        URL url = new URL(jsonUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            // Parse JSON to extract webSocketDebuggerUrl
            // Simple parsing: look for "webSocketDebuggerUrl": "ws://..." (with spaces)
            String json = response.toString();

            int keyIndex = json.indexOf("\"webSocketDebuggerUrl\"");
            if (keyIndex == -1) {
                throw new IOException("webSocketDebuggerUrl not found in response: " + json);
            }

            // Find the opening quote of the value (after the colon)
            int startIndex = json.indexOf("\"", json.indexOf(":", keyIndex)) + 1;
            if (startIndex == 0) {
                throw new IOException("Invalid JSON format for webSocketDebuggerUrl in response: " + json);
            }

            // Find the closing quote
            int endIndex = json.indexOf("\"", startIndex);
            if (endIndex == -1) {
                throw new IOException("Invalid JSON format for webSocketDebuggerUrl in response: " + json);
            }

            String wsUrl = json.substring(startIndex, endIndex);

            // Replace localhost/127.0.0.1 with actual container host
            wsUrl = wsUrl.replace("localhost", host).replace("127.0.0.1", host);

            return wsUrl;
        } finally {
            conn.disconnect();
        }
    }
}
