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

    // Initialize the HTTP client for making API requests
    public WeatherService() {
        this.client = new OkHttpClient();
    }

    // Query weather data for a specific timestamp and location
    public WeatherHandler queryWeather(String timestamp, double latitude, double longitude) {
        byte[] responseIN = null;

        // Round timestamp to the previous 15-minute interval and extract date
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(timestamp);
        ZonedDateTime roundedTime = zonedDateTime.withMinute((zonedDateTime.getMinute() / 15) * 15).withSecond(0).withNano(0);
        String date = roundedTime.toLocalDate().toString();
        int hour = roundedTime.getHour();
        int minute = roundedTime.getMinute();

        // Build the URL for querying 15-minute interval weather data
        String url = String.format(
            "https://historical-forecast-api.open-meteo.com/v1/forecast?latitude=" + latitude + "&longitude=" + longitude + "&minutely_15=temperature_2m,precipitation&start_date=%s&end_date=%s&format=flatbuffers", date, date);

        Request request = new Request.Builder()
                .url(url)
                .method("GET", null)
                .build();

        // Execute the API request and process the response
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                responseIN = response.body().bytes(); // Retrieve binary data if the request is successful
                return processWeatherData(responseIN, date, hour, minute); // Parse and return weather data
            } else {
                throw new IOException("Request failed with code: " + response.code());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Process binary weather data to retrieve temperature and precipitation
    private WeatherHandler processWeatherData(byte[] responseIN, String date, int targetHour, int targetMinute) {
        try {
            // Prepare the ByteBuffer with little-endian order for flatbuffers
            ByteBuffer buffer = ByteBuffer.wrap(responseIN).order(ByteOrder.LITTLE_ENDIAN);

            // Parse binary response into a structured WeatherApiResponse
            WeatherApiResponse mApiResponse = WeatherApiResponse.getRootAsWeatherApiResponse((ByteBuffer) buffer.position(4));

            // Retrieve 15-minute interval weather data
            VariablesWithTime minutely = mApiResponse.minutely15();

            // Extract temperature and precipitation data from the weather response
            VariableWithValues temperature = new VariablesSearch(minutely)
                    .variable(Variable.temperature)
                    .first();

            VariableWithValues precipitation = new VariablesSearch(minutely)
                    .variable(Variable.precipitation)
                    .first();

            // Calculate index for the required 15-minute interval
            int targetIndex = targetHour * 4 + targetMinute / 15; // Four 15-min intervals per hour

            double currentTemperature = temperature.values(targetIndex);
            double currentPrecipitation = precipitation.values(targetIndex);
            
            // Clear buffer after processing to free up memory
            buffer.clear();
            
            return new WeatherHandler(currentTemperature, currentPrecipitation);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
