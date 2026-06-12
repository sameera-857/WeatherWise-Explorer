package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchModule {

    private final DatabaseManager db = new DatabaseManager();
    private final RecommendationModule recModule = new RecommendationModule();

    public CityInfo searchCity(String cityName) {
        return db.getCity(cityName);
    }

    public List<PlaceInfo> searchPlaces(String cityName) {
        return recModule.recommend(cityName);
    }

    public List<PlaceInfo> searchPlaces(String cityName, String keyword) {
        List<PlaceInfo> allPlaces = recModule.recommend(cityName);
        List<PlaceInfo> matches = new ArrayList<>();

        if (allPlaces == null) {
            return matches;
        }

        String q = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);

        if (q.isEmpty()) {
            return allPlaces;
        }

        for (PlaceInfo p : allPlaces) {
            if (contains(p.getName(), q)
                    || contains(p.getCategory(), q)
                    || contains(p.getPlaceType(), q)
                    || contains(p.getDescription(), q)) {
                matches.add(p);
            }
        }

        return matches;
    }

    public void displaySearchResults(String cityName, String keyword) {
        CityInfo city = searchCity(cityName);

        if (city != null) {
            System.out.println("\nCity: " + city.getName());
            System.out.println("Location: " + city.getCountryState());
            System.out.println("About: " + city.getDescription());
        }

        List<PlaceInfo> places = searchPlaces(cityName, keyword);

        if (places == null || places.isEmpty()) {
            System.out.println("No matching places found.");
            return;
        }

        System.out.println("\nMatching Places:");
        for (PlaceInfo p : places) {
            System.out.println("- " + p.getName()
                    + " (" + p.getCategory()
                    + ", " + p.getPlaceType()
                    + ", Popularity: " + p.getPopularity() + ")");
        }
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }
}
