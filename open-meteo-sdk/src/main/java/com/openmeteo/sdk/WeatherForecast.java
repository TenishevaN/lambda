package com.openmeteo.sdk;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import java.util.List;

public class WeatherForecast {

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("generationtime_ms")
    private double generationTimeMs;

    @SerializedName("utc_offset_seconds")
    private int utcOffsetSeconds;

    @SerializedName("timezone")
    private String timezone;

    @SerializedName("timezone_abbreviation")
    private String timezoneAbbreviation;

    @SerializedName("elevation")
    private double elevation;

    @SerializedName("hourly_units")
    private Map<String, String> hourlyUnits;

    @SerializedName("hourly")
    private Map<String, List<Object>> hourly;

    @SerializedName("current_units")
    private Map<String, String> currentUnits;

    @SerializedName("current")
    private Map<String, Object> current;

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getGenerationTimeMs() {
        return generationTimeMs;
    }

    public void setGenerationTimeMs(double generationTimeMs) {
        this.generationTimeMs = generationTimeMs;
    }

    public int getUtcOffsetSeconds() {
        return utcOffsetSeconds;
    }

    public void setUtcOffsetSeconds(int utcOffsetSeconds) {
        this.utcOffsetSeconds = utcOffsetSeconds;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getTimezoneAbbreviation() {
        return timezoneAbbreviation;
    }

    public void setTimezoneAbbreviation(String timezoneAbbreviation) {
        this.timezoneAbbreviation = timezoneAbbreviation;
    }

    public double getElevation() {
        return elevation;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }

    public Map<String, String> getHourlyUnits() {
        return hourlyUnits;
    }

    public void setHourlyUnits(Map<String, String> hourlyUnits) {
        this.hourlyUnits = hourlyUnits;
    }

    public Map<String, List<Object>> getHourly() {
        return hourly;
    }

    public void setHourly(Map<String, List<Object>> hourly) {
        this.hourly = hourly;
    }

    public Map<String, String> getCurrentUnits() {
        return currentUnits;
    }

    public void setCurrentUnits(Map<String, String> currentUnits) {
        this.currentUnits = currentUnits;
    }

    public Map<String, Object> getCurrent() {
        return current;
    }

    public void setCurrent(Map<String, Object> current) {
        this.current = current;
    }
}
