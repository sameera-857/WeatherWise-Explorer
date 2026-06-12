package com.example;

public class RecommendedPlace {
    private final PlaceInfo place;
    private final int score;
    private final String reason;

    public RecommendedPlace(PlaceInfo place, int score, String reason) {
        this.place = place;
        this.score = score;
        this.reason = reason;
    }

    public PlaceInfo getPlace() {
        return place;
    }

    public int getScore() {
        return score;
    }

    public String getReason() {
        return reason;
    }
}
