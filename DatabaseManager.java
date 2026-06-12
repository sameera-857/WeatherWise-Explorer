package com.example;

import java.util.HashMap;
import java.util.Map;

public class DatabaseManager {
    private final Map<String, CityInfo> cities = new HashMap<>();

    public DatabaseManager() {
        cities.put("dehradun", new CityInfo(
                "Dehradun",
                "Uttarakhand, India",
                "Dehradun is the capital of Uttarakhand, known for valleys, caves, institutes, monasteries, and access to nearby hill stations."
        ));

        cities.put("mussoorie", new CityInfo(
                "Mussoorie",
                "Uttarakhand, India",
                "Mussoorie is a popular hill station known for viewpoints, Mall Road, waterfalls, cafes, and cool mountain weather."
        ));

        cities.put("haridwar", new CityInfo(
                "Haridwar",
                "Uttarakhand, India",
                "Haridwar is a spiritual city on the Ganga, famous for Har Ki Pauri, temples, ghats, and evening aarti."
        ));

        cities.put("rishikesh", new CityInfo(
                "Rishikesh",
                "Uttarakhand, India",
                "Rishikesh is known for yoga, rafting, temples, river views, cafes, and adventure activities."
        ));
    }

    public CityInfo getCity(String cityName) {
        if (cityName == null) {
            return null;
        }

        return cities.get(cityName.trim().toLowerCase());
    }
}
