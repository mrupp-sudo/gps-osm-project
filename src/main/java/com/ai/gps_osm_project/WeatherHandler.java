package com.ai.gps_osm_project;

public class WeatherHandler {
    private double temperature;
    private String temperatureCategory;
    private double precipitation;
    private String precipitationCategory;

    public WeatherHandler() {
        temperatureCategory = null;
        precipitationCategory = null;
    }
    
    public WeatherHandler(double temperature, double precipitation) {
        this.temperature = temperature;
        this.precipitation = precipitation;
        temperatureCategory = this.categorizeTemperature();
        precipitationCategory = this.categorizePrecipitation();
    }

    // Returns the temperature category
    public String getTemperatureCategory() {
        return temperatureCategory;
    }

    // Returns the precipitation category
    public String getPrecipitationCategory() {
        return precipitationCategory;
    }

    // Compares temperature category with the other handler, returns new category if changed
    public String getNewTemperatureCategory(WeatherHandler other) {
        if (temperatureCategory.equals(other.getTemperatureCategory())) {
            return null;
        }
        return temperatureCategory;
    }
    
    // Compares temperature category with the other handler, returns old category if changed
    public String getDeletedTemperatureCategory(WeatherHandler other) {
        if (temperatureCategory.equals(other.getTemperatureCategory())) {
            return null;
        }
        return other.getTemperatureCategory();
    }
    
    // Compares precipitation category with the other handler, returns new category if changed
    public String getNewPrecipitationCategory(WeatherHandler other) {
        if (precipitationCategory.equals(other.getPrecipitationCategory())) {
            return null;
        }
        return precipitationCategory;
    }
    
    // Compares precipitation category with the other handler, returns old category if changed
    public String getDeletedPrecipitationCategory(WeatherHandler other) {
        if (precipitationCategory.equals(other.getPrecipitationCategory())) {
            return null;
        }
        return other.getPrecipitationCategory();
    }

    // Determines temperature category based on the temperature value
    private String categorizeTemperature() {
        if (temperature < 0) {
            return "freezing";
        } else if (temperature >= 0 && temperature < 10) {
            return "cold";
        } else if (temperature >= 10 && temperature < 20) {
            return "mild";
        } else if (temperature >= 20 && temperature < 30) {
            return "warm";
        } else {
            return "hot";
        }
    }

    // Determines precipitation category based on the precipitation value
    private String categorizePrecipitation() {
        if (precipitation == 0) {
            return "no";
        } else if (precipitation < 0.5) {
            return "light";
        } else if (precipitation >= 0.5 && precipitation < 2.5) {
            return "moderate";
        } else if (precipitation >= 2.5 && precipitation < 10) {
            return "heavy";
        } else {
            return "extreme";
        }
    }
}

