package com.example;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WeatherModule {

    private static final String GEO_URL =
            "https://geocoding-api.open-meteo.com/v1/search?count=10&language=en&format=json&name=";

    public WeatherData fetchWeather(String cityName) {
        try {
            double[] coords = geocode(cityName);
            if (coords == null) return null;

            String apiUrl = "https://api.open-meteo.com/v1/forecast?latitude=" + coords[0]
                    + "&longitude=" + coords[1]
                    + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m"
                    + "&daily=weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,precipitation_probability_max"
                    + "&forecast_days=7"
                    + "&models=best_match"
                    + "&timezone=auto";

            JSONObject json = getJson(apiUrl);
            if (json == null || !json.has("current")) return null;

            JSONObject current = json.getJSONObject("current");
            String[] info = mapCode(current.optInt("weather_code", 0));

            return new WeatherData(
                    cityName,
                    info[0],
                    info[1],
                    current.optDouble("temperature_2m", 0),
                    current.optInt("relative_humidity_2m", 0),
                    current.optDouble("wind_speed_10m", 0)
            );

        } catch (Exception e) {
            return null;
        }
    }

    public List<ForecastData> fetchForecast(String cityName) {
        List<ForecastData> forecast = new ArrayList<>();

        try {
            double[] coords = geocode(cityName);
            if (coords == null) return forecast;

            String apiUrl = "https://api.open-meteo.com/v1/forecast?latitude=" + coords[0]
                    + "&longitude=" + coords[1]
                    + "&daily=weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,precipitation_probability_max"
                    + "&forecast_days=7"
                    + "&models=best_match"
                    + "&timezone=auto";

            JSONObject json = getJson(apiUrl);
            if (json == null || !json.has("daily")) return forecast;

            JSONObject daily = json.getJSONObject("daily");

            JSONArray dates = daily.getJSONArray("time");
            JSONArray codes = daily.getJSONArray("weather_code");
            JSONArray highs = daily.getJSONArray("temperature_2m_max");
            JSONArray lows = daily.getJSONArray("temperature_2m_min");
            JSONArray apparentHighs = daily.getJSONArray("apparent_temperature_max");
            JSONArray apparentLows = daily.getJSONArray("apparent_temperature_min");
            JSONArray rainChance = daily.optJSONArray("precipitation_probability_max");

            for (int i = 0; i < dates.length(); i++) {
                String[] info = mapCode(codes.optInt(i, 0));

                forecast.add(new ForecastData(
                        dates.optString(i),
                        info[0],
                        info[1],
                        highs.optDouble(i, 0),
                        lows.optDouble(i, 0),
                        apparentHighs.optDouble(i, 0),
                        apparentLows.optDouble(i, 0),
                        rainChance == null ? 0 : rainChance.optInt(i, 0)
                ));
            }

        } catch (Exception e) {
            return forecast;
        }

        return forecast;
    }

    public void displayWeather(String cityName) {
        WeatherData data = fetchWeather(cityName);

        if (data == null) {
            System.out.println("Could not fetch weather for " + cityName);
            return;
        }

        System.out.println("\n--- Current Weather: " + data.getCityName() + " ---");
        System.out.println("Condition   : " + data.getCondition() + " (" + data.getDescription() + ")");
        System.out.println("Temperature : " + data.getTemperature() + " °C");
        System.out.println("Humidity    : " + data.getHumidity() + "%");
        System.out.println("Wind Speed  : " + data.getWindSpeed() + " km/h");
    }

    public void displayForecast(String cityName) {
        List<ForecastData> forecast = fetchForecast(cityName);

        if (forecast.isEmpty()) {
            System.out.println("Could not fetch forecast for " + cityName);
            return;
        }

        System.out.println("\n--- 7-Day Forecast: " + cityName + " ---");

        for (ForecastData day : forecast) {
            System.out.println(
                    day.getDate()
                            + " | " + day.getCondition()
                            + " | " + day.getMinTemp() + "°C - " + day.getMaxTemp() + "°C"
                            + " | Feels: " + day.getApparentMinTemp() + "°C - " + day.getApparentMaxTemp() + "°C"
                            + " | Rain: " + day.getPrecipitationProbability() + "%"
            );
        }
    }

    private double[] geocode(String name) throws Exception {
        JSONObject geo = getJson(GEO_URL + URLEncoder.encode(name, StandardCharsets.UTF_8));
        if (geo == null || !geo.has("results")) return null;

        JSONArray results = geo.getJSONArray("results");
        if (results.length() == 0) return null;

        JSONObject best = results.getJSONObject(0);

        for (int i = 0; i < results.length(); i++) {
            JSONObject result = results.getJSONObject(i);
            if ("IN".equalsIgnoreCase(result.optString("country_code"))) {
                best = result;
                break;
            }
        }

        return new double[]{
                best.optDouble("latitude", 0),
                best.optDouble("longitude", 0)
        };
    }

    private JSONObject getJson(String urlStr) throws Exception {
        URL url = URI.create(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        if (conn.getResponseCode() != 200) return null;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            return new JSONObject(sb.toString());
        }
    }

    private String[] mapCode(int code) {
        if (code == 0) return new String[]{"Clear", "clear sky"};
        if (code == 1) return new String[]{"Mostly Clear", "mainly clear"};
        if (code == 2) return new String[]{"Cloudy", "partly cloudy"};
        if (code == 3) return new String[]{"Overcast", "overcast"};
        if (code == 45 || code == 48) return new String[]{"Fog", "foggy conditions"};
        if (code >= 51 && code <= 57) return new String[]{"Drizzle", "light drizzle"};
        if (code >= 61 && code <= 67) return new String[]{"Rain", "rainy weather"};
        if (code >= 71 && code <= 77) return new String[]{"Snow", "snowfall"};
        if (code >= 80 && code <= 82) return new String[]{"Showers", "rain showers"};
        if (code >= 95) return new String[]{"Storm", "thunderstorm"};

        return new String[]{"Fair", "stable conditions"};
    }
}
