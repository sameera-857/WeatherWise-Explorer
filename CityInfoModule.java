package com.example;

public class CityInfoModule {

    private final DatabaseManager db = new DatabaseManager();

    public CityInfo getCityInfo(String cityName) {
        return db.getCity(cityName);
    }

    public void displayCityInfo(String cityName) {
        CityInfo info = getCityInfo(cityName);

        if (info == null) {
            System.out.println("City not found: " + cityName);
            return;
        }

        System.out.println("\n--- City Info ---");
        System.out.println("Name     : " + info.getName());
        System.out.println("Location : " + info.getCountryState());
        System.out.println("About    : " + info.getDescription());
    }
}
