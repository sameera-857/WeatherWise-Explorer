package com.example;

import java.util.List;
import java.util.Scanner;

public class UI {

    private final Scanner scanner = new Scanner(System.in);
    private final WeatherModule weatherModule = new WeatherModule();
    private final CityInfoModule cityModule = new CityInfoModule();
    private final RecommendationModule recModule = new RecommendationModule();
    private final SearchModule searchModule = new SearchModule();

    public void run() {
        System.out.println("\nWelcome to WeatherWise Explorer!");

        boolean running = true;

        while (running) {
            System.out.println("\n1. Current Weather");
            System.out.println("2. Weather Forecast");
            System.out.println("3. City Info");
            System.out.println("4. Recommendations");
            System.out.println("5. Search Places");
            System.out.println("6. Exit");
            System.out.print("Choose: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    showWeather();
                    break;

                case "2":
                    showForecast();
                    break;

                case "3":
                    cityModule.displayCityInfo(askCity());
                    break;

                case "4":
                    showRecommendations();
                    break;

                case "5":
                    showSearch();
                    break;

                case "6":
                    running = false;
                    System.out.println("Goodbye!");
                    break;

                default:
                    System.out.println("Invalid choice");
                    break;
            }
        }
    }

    private String askCity() {
        System.out.print("Enter city: ");
        return scanner.nextLine().trim();
    }

    private void showWeather() {
        String city = askCity();
        WeatherData data = weatherModule.fetchWeather(city);

        if (data == null) {
            System.out.println("Could not fetch weather for " + city);
            return;
        }

        System.out.println("\n--- Current Weather ---");
        System.out.println("City        : " + data.getCityName());
        System.out.println("Condition   : " + data.getCondition());
        System.out.println("Description : " + data.getDescription());
        System.out.println("Temperature : " + data.getTemperature() + " °C");
        System.out.println("Humidity    : " + data.getHumidity() + "%");
        System.out.println("Wind Speed  : " + data.getWindSpeed() + " km/h");
    }

    private void showForecast() {
        String city = askCity();
        List<ForecastData> forecast = weatherModule.fetchForecast(city);

        if (forecast == null || forecast.isEmpty()) {
            System.out.println("Could not fetch forecast for " + city);
            return;
        }

        System.out.println("\n--- 7-Day Forecast: " + city + " ---");

        for (ForecastData f : forecast) {
            System.out.println(
                    f.getDate()
                            + " | " + f.getCondition()
                            + " | " + f.getMinTemp() + "°C - " + f.getMaxTemp() + "°C"
                            + " | Rain: " + f.getPrecipitationProbability() + "%"
            );
        }
    }

    private void showRecommendations() {
        String city = askCity();
        List<PlaceInfo> list = recModule.recommend(city);

        if (list == null || list.isEmpty()) {
            System.out.println("No recommendations found.");
            return;
        }

        System.out.println("\nRecommended Places:");

        for (PlaceInfo p : list) {
            System.out.println("- " + p.getName()
                    + " (" + p.getCategory()
                    + ", " + p.getPlaceType()
                    + ", Popularity: " + p.getPopularity() + ")");
        }
    }

    private void showSearch() {
        String city = askCity();

        System.out.print("Search keyword: ");
        String keyword = scanner.nextLine().trim();

        searchModule.displaySearchResults(city, keyword);
    }

    public static void main(String[] args) {
        new UI().run();
    }
}
