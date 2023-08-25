package uk.ac.cam.cl.dtg.segue.api.services;

import org.json.JSONObject;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class SimpleHttpClientService {

    public JSONObject post(final String url, final String params) throws IOException {
        HttpURLConnection http = createConnection(url, "POST");
        setRequestProperties(http, "application/x-www-form-urlencoded; charset=UTF-8");
        sendRequestData(http, params);

        String response = getResponse(http);
        return new JSONObject(response);
    }

    private HttpURLConnection createConnection(final String url, final String requestMethod) throws IOException {
        HttpURLConnection http = (HttpURLConnection) new URL(url).openConnection();
        http.setRequestMethod(requestMethod);
        return http;
    }

    private void setRequestProperties(final HttpURLConnection http, final String contentType) {
        http.setDoOutput(true);
        http.setRequestProperty("Content-Type", contentType);
    }

    private void sendRequestData(final HttpURLConnection http, final String params) throws IOException {
        try (OutputStream out = http.getOutputStream()) {
            out.write(params.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    private String getResponse(final HttpURLConnection http) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (InputStream res = http.getInputStream();
             BufferedReader rd = new BufferedReader(new InputStreamReader(res, StandardCharsets.UTF_8))) {

            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
        }
        return sb.toString();
    }
}
