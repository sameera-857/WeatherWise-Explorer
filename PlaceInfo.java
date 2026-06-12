package com.example;

public class PlaceInfo {
    private final String name;
    private final String category;
    private final String placeType;
    private final int popularity;
    private final String description;
    private final String imagePath;

    public PlaceInfo(String name, String category) {
        this(name, category, "Outdoor", 50, "", "");
    }

    public PlaceInfo(String name, String category, String placeType, int popularity) {
        this(name, category, placeType, popularity, "", "");
    }

    public PlaceInfo(String name, String category, String placeType, int popularity, String description) {
        this(name, category, placeType, popularity, description, "");
    }

    public PlaceInfo(String name, String category, String placeType, int popularity, String description, String imagePath) {
        this.name = name;
        this.category = category;
        this.placeType = placeType;
        this.popularity = popularity;
        this.description = description;
        this.imagePath = imagePath;
    }

    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getPlaceType() { return placeType; }
    public int getPopularity() { return popularity; }
    public String getDescription() { return description; }
    public String getImagePath() { return imagePath; }

    public boolean isIndoor() {
        return "Indoor".equalsIgnoreCase(placeType);
    }

    public boolean isOutdoor() {
        return "Outdoor".equalsIgnoreCase(placeType);
    }
}
