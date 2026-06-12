package com.example;

public class ForecastData {
    private final String date;
    private final String condition;
    private final String description;
    private final double maxTemp;
    private final double minTemp;
    private final double apparentMaxTemp;
    private final double apparentMinTemp;
    private final int precipitationProbability;

    public ForecastData(String date, String condition, String description,
                        double maxTemp, double minTemp,
                        double apparentMaxTemp, double apparentMinTemp,
                        int precipitationProbability) {
        this.date = date;
        this.condition = condition;
        this.description = description;
        this.maxTemp = maxTemp;
        this.minTemp = minTemp;
        this.apparentMaxTemp = apparentMaxTemp;
        this.apparentMinTemp = apparentMinTemp;
        this.precipitationProbability = precipitationProbability;
    }

    public String getDate() { return date; }
    public String getCondition() { return condition; }
    public String getDescription() { return description; }
    public double getMaxTemp() { return maxTemp; }
    public double getMinTemp() { return minTemp; }
    public double getApparentMaxTemp() { return apparentMaxTemp; }
    public double getApparentMinTemp() { return apparentMinTemp; }
    public int getPrecipitationProbability() { return precipitationProbability; }
}
