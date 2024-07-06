package com.openmeteo.sdk;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class OpenMeteoClient {
    private static final String API_URL = "https://api.open-meteo.com/v1/forecast?latitude=52.52&longitude=13.41&current=temperature_2m,wind_speed_10m&hourly=temperature_2m,relative_humidity_2m,wind_speed_10m";

    public WeatherForecast getLatestForecast() {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = new HttpGet(API_URL);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String jsonResponse = EntityUtils.toString(response.getEntity());
            return new Gson().fromJson(jsonResponse, WeatherForecast.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to get forecast", e);
        }
    }
}