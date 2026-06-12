package com.example;

public class CityInfo {
    private final String name;
    private final String countryState;
    private final String description;

    public CityInfo(String name, String countryState, String description) {
        this.name = name;
        this.countryState = countryState;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getCountryState() {
        return countryState;
    }

    public String getDescription() {
        return description;
    }
}
