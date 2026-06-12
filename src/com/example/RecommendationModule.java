package com.example;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class RecommendationModule {

    public List<PlaceInfo> recommend(String city) {
        return placesForCity(city);
    }

    public List<RecommendedPlace> recommend(String city, WeatherData weather, List<ForecastData> forecast) {
        List<PlaceInfo> places = placesForCity(city);
        List<RecommendedPlace> result = new ArrayList<>();

        String condition = weather == null ? "" : safe(weather.getCondition()).toLowerCase(Locale.ROOT);
        double temperature = weather == null ? 24 : weather.getTemperature();
        int humidity = weather == null ? 50 : weather.getHumidity();
        int rainChance = getTodayRainChance(forecast);

        for (PlaceInfo place : places) {
            int score = place.getPopularity();
            String reason = "Good general recommendation for this city.";

            String category = safe(place.getCategory()).toLowerCase(Locale.ROOT);
            String type = safe(place.getPlaceType()).toLowerCase(Locale.ROOT);

            boolean indoor = type.contains("indoor");
            boolean outdoor = type.contains("outdoor");

            if (isRainy(condition) || rainChance >= 45) {
                if (indoor) {
                    score += 35;
                    reason = "Rain chance is high, so indoor places are better today.";
                } else {
                    score -= 25;
                    reason = "Outdoor visit may be affected by rain today.";
                }
            } else if (isClear(condition)) {
                if (outdoor) {
                    score += 30;
                    reason = "Clear weather is good for outdoor sightseeing.";
                } else {
                    score += 5;
                    reason = "Weather is clear, but this is still a comfortable option.";
                }
            }

            if (temperature >= 32) {
                if (indoor || category.contains("water") || category.contains("cave")) {
                    score += 25;
                    reason = "It is hot today, so cooler or indoor places are better.";
                } else if (category.contains("trek") || category.contains("viewpoint")) {
                    score -= 20;
                    reason = "It is hot today, so long outdoor activity may be tiring.";
                }
            }

            if (temperature <= 14) {
                if (indoor || category.contains("cafe") || category.contains("market")) {
                    score += 20;
                    reason = "Cool weather makes indoor, cafe, and market visits more comfortable.";
                } else {
                    score -= 10;
                    reason = "It is quite cool, so outdoor plans may need warm clothing.";
                }
            }

            if (humidity >= 80) {
                if (indoor) {
                    score += 15;
                    reason = "Humidity is high, so indoor places may feel better.";
                } else {
                    score -= 10;
                }
            }

            if (condition.contains("fog")) {
                if (category.contains("viewpoint") || category.contains("trek")) {
                    score -= 35;
                    reason = "Fog can reduce visibility, so viewpoints and treks are not ideal.";
                } else if (indoor) {
                    score += 25;
                    reason = "Foggy weather makes indoor places safer and more comfortable.";
                }
            }

            score = Math.max(0, Math.min(score, 100));
            result.add(new RecommendedPlace(place, score, reason));
        }

        result.sort(Comparator.comparingInt(RecommendedPlace::getScore).reversed());
        return result;
    }

    private int getTodayRainChance(List<ForecastData> forecast) {
        if (forecast == null || forecast.isEmpty()) {
            return 0;
        }
        return forecast.get(0).getPrecipitationProbability();
    }

    private boolean isRainy(String condition) {
        return condition.contains("rain")
                || condition.contains("drizzle")
                || condition.contains("shower")
                || condition.contains("storm");
    }

    private boolean isClear(String condition) {
        return condition.contains("clear")
                || condition.contains("sun")
                || condition.contains("fair")
                || condition.contains("mostly clear");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private List<PlaceInfo> placesForCity(String city) {
        String c = city == null ? "" : city.trim().toLowerCase(Locale.ROOT);

        if (c.contains("mussoorie")) {
            return mussooriePlaces();
        }

        if (c.contains("haridwar")) {
            return haridwarPlaces();
        }

        if (c.contains("rishikesh")) {
            return rishikeshPlaces();
        }

        return dehradunPlaces();
    }

    private List<PlaceInfo> dehradunPlaces() {
        List<PlaceInfo> places = new ArrayList<>();

        places.add(new PlaceInfo("Robber's Cave", "Cave / Water", "Outdoor", 92,
                "Scenic cave stream, best in clear or warm weather.", "/images/robbers-cave.jpg"));
        places.add(new PlaceInfo("Sahastradhara", "Water / Nature", "Outdoor", 88,
                "Sulphur springs and waterfalls, good for warm days.", "/images/sahastradhara.jpg"));
        places.add(new PlaceInfo("Forest Research Institute", "Museum / Heritage", "Indoor", 84,
                "Good indoor and semi-outdoor option with heritage buildings.", "/images/fri.jpg"));
        places.add(new PlaceInfo("Mindrolling Monastery", "Spiritual / Culture", "Indoor", 78,
                "Peaceful cultural visit, suitable in most weather.", "/images/mindrolling-monastery.jpg"));
        places.add(new PlaceInfo("Pacific Mall", "Shopping / Food", "Indoor", 72,
                "Best during rain, heat, or humid weather.", "/images/pacific-mall.jpg"));
        places.add(new PlaceInfo("Clock Tower Market", "Market / Food", "Indoor", 70,
                "Good for shopping and food when weather is not ideal.", "/images/clock-tower-market.jpg"));

        return places;
    }

    private List<PlaceInfo> mussooriePlaces() {
        List<PlaceInfo> places = new ArrayList<>();

        places.add(new PlaceInfo("Mall Road", "Market / Walk", "Outdoor", 88,
                "Best in clear or cool weather.", "/images/mall-road-mussoorie.jpg"));
        places.add(new PlaceInfo("Kempty Falls", "Waterfall / Nature", "Outdoor", 84,
                "Good in clear weather, avoid during heavy rain.", "/images/kempty-falls.jpg"));
        places.add(new PlaceInfo("Gun Hill", "Viewpoint", "Outdoor", 78,
                "Best when visibility is clear.", "/images/gun-hill.jpg"));
        places.add(new PlaceInfo("Lal Tibba", "Viewpoint / Trek", "Outdoor", 82,
                "Great views, avoid foggy weather.", "/images/lal-tibba.jpg"));
        places.add(new PlaceInfo("Landour Bakehouse", "Cafe / Food", "Indoor", 80,
                "Excellent for cold, foggy, or rainy weather.", "/images/landour-bakehouse.jpg"));
        places.add(new PlaceInfo("Tibetan Market", "Shopping", "Indoor", 68,
                "Good indoor shopping option.", "/images/tibetan-market-mussoorie.jpg"));

        return places;
    }

    private List<PlaceInfo> haridwarPlaces() {
        List<PlaceInfo> places = new ArrayList<>();

        places.add(new PlaceInfo("Har Ki Pauri", "Spiritual / Ghat", "Outdoor", 95,
                "Best in clear weather, especially evening aarti.", "/images/har-ki-pauri.jpg"));
        places.add(new PlaceInfo("Mansa Devi Temple", "Temple / Viewpoint", "Outdoor", 86,
                "Good in clear weather, can be tiring in high heat.", "/images/mansa-devi-temple.jpg"));
        places.add(new PlaceInfo("Chandi Devi Temple", "Temple / Cable Car", "Outdoor", 84,
                "Best when visibility is good.", "/images/chandi-devi-temple.jpg"));
        places.add(new PlaceInfo("Rajaji National Park", "Wildlife / Safari", "Outdoor", 80,
                "Avoid during rain or extreme heat.", "/images/rajaji-national-park.jpg"));
        places.add(new PlaceInfo("ISKCON Temple", "Temple / Spiritual", "Indoor", 76,
                "Good for hot or rainy weather.", "/images/iskcon-haridwar.jpg"));
        places.add(new PlaceInfo("Patanjali Yog Peeth", "Wellness", "Indoor", 72,
                "Comfortable indoor wellness visit.", "/images/patanjali-yogpeeth.jpg"));

        return places;
    }

    private List<PlaceInfo> rishikeshPlaces() {
        List<PlaceInfo> places = new ArrayList<>();

        places.add(new PlaceInfo("Laxman Jhula", "Bridge / Walk", "Outdoor", 90,
                "Best in clear weather.", "/images/laxman-jhula.jpg"));
        places.add(new PlaceInfo("White Water Rafting", "Adventure / Water", "Outdoor", 92,
                "Avoid during storms or heavy rain.", "/images/white-water-rafting.jpg"));
        places.add(new PlaceInfo("Neer Garh Waterfall", "Waterfall / Trek", "Outdoor", 82,
                "Good in clear weather, avoid heavy rain.", "/images/neer-garh-waterfall.jpg"));
        places.add(new PlaceInfo("Beatles Ashram", "Culture / Museum", "Indoor", 78,
                "Good in most weather.", "/images/beatles-ashram.jpg"));
        places.add(new PlaceInfo("Parmarth Niketan", "Yoga / Spiritual", "Indoor", 80,
                "Great for rain, heat, or calm evenings.", "/images/parmarth-niketan.jpg"));
        places.add(new PlaceInfo("The Great Cafe", "Cafe / Food", "Indoor", 70,
                "Good backup for rainy or hot weather.", "/images/the-great-cafe-rishikesh.jpg"));

        return places;
    }
}
