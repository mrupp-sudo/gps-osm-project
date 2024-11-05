package com.ai.gps_osm_project;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.openmeteo.sdk.Variable;
import com.openmeteo.sdk.VariableWithValues;
import com.openmeteo.sdk.VariablesSearch;
import com.openmeteo.sdk.VariablesWithTime;
import com.openmeteo.sdk.WeatherApiResponse;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.ZonedDateTime;

public class WeatherService {

    private final OkHttpClient client;

    // Constructor to initialize OkHttpClient
    public WeatherService() {
        this.client = new OkHttpClient();
    }

    // Fetch weather data synchronously
    public WeatherHandler queryWeather(String timestamp, double latitude, double longitude) {
        byte[] responseIN = null;

        // Parse and round the timestamp to the nearest 15-minute interval
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(timestamp);
        ZonedDateTime roundedTime = zonedDateTime.withMinute((zonedDateTime.getMinute() / 15) * 15).withSecond(0).withNano(0);
        // Extract date in YYYY-MM-DD format
        String date = roundedTime.toLocalDate().toString();
        int hour = roundedTime.getHour();
        int minute = roundedTime.getMinute();

        // Define the API URL for the weather forecast with minutely_15 data
        String url = String.format(
            "https://historical-forecast-api.open-meteo.com/v1/forecast?latitude=" + latitude + "&longitude=" + longitude + "&minutely_15=temperature_2m,precipitation&start_date=%s&end_date=%s&format=flatbuffers", date, date);

        Request request = new Request.Builder()
                .url(url)
                .method("GET", null)
                .build();

        // Use the synchronous `execute` method to make the request
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                responseIN = response.body().bytes(); // Get the binary response
                return processWeatherData(responseIN, date, hour, minute); // Parse and return the weather data
            } else {
                throw new IOException("Request failed with code: " + response.code());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private WeatherHandler processWeatherData(byte[] responseIN, String date, int targetHour, int targetMinute) {
        try {
            // Convert the binary response to ByteBuffer with little-endian order
            ByteBuffer buffer = ByteBuffer.wrap(responseIN).order(ByteOrder.LITTLE_ENDIAN);

            // Interpret the ByteBuffer as a WeatherApiResponse (positioned at 4)
            WeatherApiResponse mApiResponse = WeatherApiResponse.getRootAsWeatherApiResponse((ByteBuffer) buffer.position(4));

            // Retrieve the 15-minutely weather data block
            VariablesWithTime minutely = mApiResponse.minutely15();

            // Extract temperature and weather code variables
            VariableWithValues temperature = new VariablesSearch(minutely)
                    .variable(Variable.temperature)
                    .first();

            VariableWithValues precipitation = new VariablesSearch(minutely)
                    .variable(Variable.precipitation)
                    .first();

            // Calculate the index for the 15-minute interval (e.g., 11:30 -> index 46 if data starts at 00:00)
            int targetIndex = targetHour * 4 + targetMinute / 15; // Each hour has 4 intervals (15 minutes each)

            double currentTemperature = temperature.values(targetIndex);
            double currentPrecipitation = precipitation.values(targetIndex);
            
            // Clear the buffer after use
            buffer.clear();
            
            return new WeatherHandler(currentTemperature, currentPrecipitation);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}