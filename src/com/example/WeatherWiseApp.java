package com.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WeatherWiseApp extends Application {

    private static final String BG_BASE = "#0d1117";
    private static final String BG_CARD = "#161b22";
    private static final String BG_CARD_2 = "#1c2128";
    private static final String BG_ACCENT = "#21262d";
    private static final String BORDER = "#30363d";
    private static final String TEXT_PRIMARY = "#e6edf3";
    private static final String TEXT_SECONDARY = "#8b949e";
    private static final String TEXT_MUTED = "#6e7681";
    private static final String BLUE = "#58a6ff";
    private static final String GREEN = "#3fb950";
    private static final String AMBER = "#d29922";
    private static final String RED = "#f85149";
    private static final String PURPLE = "#a5a0f7";

    private WeatherModule weatherModule;
    private DatabaseManager db;
    private RecommendationModule recModule;

    private final Map<String, DashboardCity> cities = buildCities();
    private String activeCityKey = "dehradun";
    private String activeRecommendationTab = "outdoor";
    private String activeChartTab = "temp";
    private Button activeNavButton;
    private ScrollPane dashboardScroll;

    private Label dateLabel;
    private Label cityLabel;
    private Label tempLabel;
    private Label conditionLabel;
    private Label iconLabel;
    private Label windLabel;
    private Label humidityLabel;
    private Label visibilityLabel;
    private Label alertBanner;
    private Label tomorrowLabel;
    private Label tomorrowTempLabel;
    private Label recTitleLabel;
    private Label recSubtitleLabel;
    private Label mapCityLabel;
    private Label mapConditionLabel;
    private Label mapTempLabel;
    private Label aqiLabel;
    private Label uvLabel;
    private Label precipLabel;
    private Label weatherTagLabel;

    private VBox cityListBox;
    private VBox forecastRows;
    private GridPane placesGrid;
    private VBox topPicksBox;
    private StackPane chartHolder;
    private HBox recommendationTabs;
    private final Map<String, Button> chartButtons = new LinkedHashMap<>();
    private final Map<String, Button> recommendationButtons = new LinkedHashMap<>();
    private final Map<String, List<PlaceCard>> backendRecommendationCache = new LinkedHashMap<>();

    @Override
    public void start(Stage stage) {
        BorderPane app = new BorderPane();
        app.setStyle("-fx-background-color:" + BG_BASE + ";");
        app.setLeft(buildSidebar());
        app.setCenter(buildMainArea());

        refreshDashboard(activeCityKey);

        Scene scene = new Scene(app, 1180, 720);
        stage.setTitle("WeatherWise Explorer");
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
    }

    private Node buildSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setAlignment(Pos.TOP_CENTER);
        sidebar.setPadding(new Insets(20, 0, 20, 0));
        sidebar.setPrefWidth(72);
        sidebar.setStyle("-fx-background-color:" + BG_CARD + "; -fx-border-color: transparent " + BORDER + " transparent transparent;");

        Label logo = new Label("⛰");
        logo.setAlignment(Pos.CENTER);
        logo.setFont(Font.font(18));
        logo.setPrefSize(38, 38);
        logo.setStyle("-fx-background-color: linear-gradient(to bottom right, #4f8ef7, #a5a0f7); -fx-background-radius: 10;");

        Button dashboardButton = navButton("⌂", "Dashboard", true, () -> scrollTo(0));
        Button mapButton = navButton("⌖", "Map", false, () -> scrollTo(0));
        Button forecastButton = navButton("▦", "Forecast", false, () -> scrollTo(0.18));
        Button placesButton = navButton("★", "Places", false, () -> scrollTo(0.95));
        Button alertsButton = navButton("!", "Alerts", false, this::showAlertCity);
        sidebar.getChildren().addAll(logo, dashboardButton, mapButton, forecastButton, placesButton, alertsButton);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Label avatar = avatar("EX", 36);
        sidebar.getChildren().addAll(spacer, navButton("⚙", "Settings", false, this::searchFocusMessage), avatar);
        return sidebar;
    }

    private Node buildMainArea() {
        BorderPane main = new BorderPane();
        main.setTop(buildTopbar());

        dashboardScroll = new ScrollPane(buildContent());
        dashboardScroll.setFitToWidth(true);
        dashboardScroll.setStyle("-fx-background:" + BG_BASE + "; -fx-background-color:" + BG_BASE + ";");
        main.setCenter(dashboardScroll);
        return main;
    }

    private Node buildTopbar() {
        HBox topbar = new HBox(14);
        topbar.setAlignment(Pos.CENTER_LEFT);
        topbar.setPadding(new Insets(0, 28, 0, 28));
        topbar.setPrefHeight(64);
        topbar.setStyle("-fx-background-color:" + BG_CARD + "; -fx-border-color: transparent transparent " + BORDER + " transparent;");

        TextField search = new TextField();
        search.setPromptText("Search city...");
        search.setPrefWidth(340);
        search.setStyle(inputStyle());
        search.textProperty().addListener((obs, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.trim().toLowerCase(Locale.ROOT);
            if (query.isEmpty()) {
                return;
            }
            cities.entrySet().stream()
                    .filter(entry -> entry.getValue().name.toLowerCase(Locale.ROOT).contains(query))
                    .findFirst()
                    .ifPresent(entry -> refreshDashboard(entry.getKey()));
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topbar.getChildren().addAll(search, spacer, iconButton("☀"), iconButton("🔔"), mutedText("Hi, Explorer 👋"), avatar("EX", 36));
        return topbar;
    }

    private Node buildContent() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(28));
        grid.setHgap(18);
        grid.setVgap(18);
        grid.setStyle("-fx-background-color:" + BG_BASE + ";");

        ColumnConstraints left = new ColumnConstraints(300);
        ColumnConstraints center = new ColumnConstraints();
        center.setHgrow(Priority.ALWAYS);
        center.setMinWidth(360);
        grid.getColumnConstraints().addAll(left, center);

        grid.add(buildCitiesColumn(), 0, 0);
        grid.add(buildCenterColumn(), 1, 0);
        return grid;
    }

    private Node buildCitiesColumn() {
        VBox col = new VBox(12);
        col.getChildren().addAll(sectionLabel("Selected City"), buildMainWeatherCard(), sectionLabel("Other Cities"));

        cityListBox = new VBox(10);
        col.getChildren().add(cityListBox);

        Button addCity = new Button("＋  Add a city");
        addCity.setMaxWidth(Double.MAX_VALUE);
        addCity.setStyle("-fx-background-color:" + BG_CARD + "; -fx-border-color:" + BORDER + "; -fx-border-style: dashed; -fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill:" + TEXT_MUTED + "; -fx-padding: 14 16;");

        HBox metrics = new HBox(10, metricTile("AQI", aqiLabel = valueLabel(GREEN), "Moderate"), metricTile("UV Index", uvLabel = valueLabel(AMBER), "High"), metricTile("Precip.", precipLabel = valueLabel(BLUE), "Chance"));
        col.getChildren().addAll(addCity, metrics);
        return col;
    }

    private Node buildMainWeatherCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(22));
        card.setStyle("-fx-background-color: linear-gradient(to bottom right, #1a3a5c, #0d2035); -fx-background-radius: 14; -fx-border-color: rgba(88,166,255,0.35); -fx-border-radius: 14;");

        dateLabel = mutedText("");
        cityLabel = primaryText("");

        HBox tempRow = new HBox(16);
        tempRow.setAlignment(Pos.TOP_LEFT);
        VBox tempBox = new VBox(8);
        tempLabel = new Label();
        tempLabel.setFont(Font.font("Georgia", FontWeight.NORMAL, 52));
        tempLabel.setTextFill(Color.WHITE);
        conditionLabel = mutedText("");
        tempBox.getChildren().addAll(tempLabel, conditionLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        iconLabel = new Label();
        iconLabel.setFont(Font.font(48));
        tempRow.getChildren().addAll(tempBox, spacer, iconLabel);

        HBox stats = new HBox(16, stat("Wind", windLabel = smallValue()), stat("Humidity", humidityLabel = smallValue()), stat("Visibility", visibilityLabel = smallValue()));
        stats.setPadding(new Insets(14, 0, 0, 0));
        stats.setStyle("-fx-border-color: rgba(255,255,255,0.12) transparent transparent transparent;");

        card.getChildren().addAll(dateLabel, cityLabel, tempRow, stats);
        return card;
    }

    private Node buildCenterColumn() {
        VBox col = new VBox(18);

        alertBanner = new Label();
        alertBanner.setWrapText(true);
        alertBanner.setMaxWidth(Double.MAX_VALUE);
        alertBanner.setStyle("-fx-background-color: rgba(210,153,34,0.12); -fx-border-color: rgba(210,153,34,0.35); -fx-background-radius: 8; -fx-border-radius: 8; -fx-padding: 10 14; -fx-text-fill:" + AMBER + ";");

        VBox forecastCard = card();
        forecastRows = new VBox();
        HBox forecastHeader = header("Featured Places", "Weather matched");
        HBox tomorrow = new HBox();
        tomorrow.setAlignment(Pos.CENTER_LEFT);
        tomorrow.setPadding(new Insets(10, 14, 10, 14));
        tomorrow.setStyle("-fx-background-color: rgba(88,166,255,0.12); -fx-border-color: rgba(88,166,255,0.35); -fx-background-radius: 8; -fx-border-radius: 8;");
        tomorrowLabel = new Label();
        tomorrowLabel.setTextFill(Color.web(BLUE));
        Region tomorrowSpacer = new Region();
        HBox.setHgrow(tomorrowSpacer, Priority.ALWAYS);
        tomorrowTempLabel = primaryText("");
        tomorrow.getChildren().addAll(tomorrowLabel, tomorrowSpacer, tomorrowTempLabel);
        forecastCard.getChildren().addAll(forecastHeader, forecastRows);

        VBox overview = card();
        HBox chartHeader = new HBox(10);
        chartHeader.setAlignment(Pos.CENTER_LEFT);
        Label chartTitle = primaryText("Overview");
        Region chartSpacer = new Region();
        HBox.setHgrow(chartSpacer, Priority.ALWAYS);
        chartHeader.getChildren().addAll(chartTitle, chartSpacer, chartTab("Temperature", "temp"), chartTab("Humidity", "humidity"), chartTab("Rainfall", "rain"));
        chartHolder = new StackPane();
        chartHolder.setPrefHeight(180);
        overview.getChildren().addAll(chartHeader, chartHolder);

        VBox recCard = card();
        recTitleLabel = primaryText("");
        recSubtitleLabel = mutedText("");
        VBox recTitleBox = new VBox(3, recTitleLabel, recSubtitleLabel);
        recommendationTabs = new HBox(6, recommendationTab("🌿 Outdoor", "outdoor"), recommendationTab("🏛 Indoor", "indoor"));
        HBox recHeader = new HBox(10, recTitleBox, new Region(), recommendationTabs);
        HBox.setHgrow(recHeader.getChildren().get(1), Priority.ALWAYS);
        placesGrid = new GridPane();
        placesGrid.setHgap(10);
        placesGrid.setVgap(10);
        recCard.getChildren().addAll(recHeader, placesGrid);

        col.getChildren().addAll(alertBanner, forecastCard, overview, recCard);
        return col;
    }

    private Node buildRightColumn() {
        VBox col = new VBox(16);
        col.getChildren().addAll(buildMapCard(), buildTopPicksCard());
        return col;
    }

    private Node buildMapCard() {
        StackPane map = new StackPane();
        map.setPrefHeight(200);
        map.setStyle("-fx-background-color:#0d1f2d; -fx-background-radius:14; -fx-border-color:" + BORDER + "; -fx-border-radius:14;");

        Polygon mountains1 = new Polygon(0.0, 140.0, 30.0, 90.0, 60.0, 110.0, 90.0, 75.0, 120.0, 95.0, 150.0, 65.0, 180.0, 85.0, 210.0, 60.0, 240.0, 80.0, 260.0, 70.0, 260.0, 140.0);
        mountains1.setFill(Color.web("#0f2535"));
        Polygon mountains2 = new Polygon(0.0, 160.0, 20.0, 120.0, 50.0, 135.0, 80.0, 105.0, 110.0, 120.0, 140.0, 95.0, 170.0, 115.0, 200.0, 90.0, 230.0, 110.0, 260.0, 95.0, 260.0, 160.0);
        mountains2.setFill(Color.web("#0a1c2a"));

        StackPane drawing = new StackPane(mountains1, mountains2, pinLayer());
        drawing.setPadding(new Insets(20, 0, 0, 0));

        VBox overlay = new VBox(2);
        overlay.setPadding(new Insets(8, 12, 8, 12));
        overlay.setStyle("-fx-background-color: rgba(13,17,23,0.9); -fx-background-radius: 8; -fx-border-color:" + BORDER + "; -fx-border-radius: 8;");
        mapCityLabel = primaryText("");
        mapConditionLabel = mutedText("");
        overlay.getChildren().addAll(mapCityLabel, mapConditionLabel);
        StackPane.setAlignment(overlay, Pos.BOTTOM_LEFT);
        StackPane.setMargin(overlay, new Insets(0, 0, 12, 12));

        mapTempLabel = new Label();
        mapTempLabel.setFont(Font.font("Georgia", 17));
        mapTempLabel.setTextFill(Color.web(TEXT_PRIMARY));
        mapTempLabel.setPadding(new Insets(5, 10, 5, 10));
        mapTempLabel.setStyle("-fx-background-color: rgba(13,17,23,0.9); -fx-background-radius: 8; -fx-border-color:" + BORDER + "; -fx-border-radius: 8;");
        StackPane.setAlignment(mapTempLabel, Pos.TOP_RIGHT);
        StackPane.setMargin(mapTempLabel, new Insets(12, 12, 0, 0));

        map.getChildren().addAll(drawing, overlay, mapTempLabel);
        return map;
    }

    private Node pinLayer() {
        StackPane layer = new StackPane();
        layer.getChildren().addAll(
                mapLine(-70, 20, -50, 0),
                mapLine(-50, 0, -20, 24),
                pin("Dehradun", BLUE, -78, 20),
                pin("Mussoorie", PURPLE, -55, 0),
                pin("Rishikesh", AMBER, -18, 26),
                pin("Haridwar", GREEN, 10, 38)
        );
        return layer;
    }

    private Node buildTopPicksCard() {
        VBox card = card();
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = primaryText("Top Picks");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        weatherTagLabel = pill("Good Weather", GREEN, "rgba(63,185,80,0.12)");
        header.getChildren().addAll(title, spacer, weatherTagLabel);
        topPicksBox = new VBox();
        card.getChildren().addAll(header, topPicksBox);
        return card;
    }

    private void refreshDashboard(String cityKey) {
        activeCityKey = cityKey;
        DashboardCity city = cities.get(cityKey);
        mergeBackendWeather(city);

        dateLabel.setText("Today, " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        cityLabel.setText("⌖ " + city.name);
        tempLabel.setText(city.temperature + "°C");
        conditionLabel.setText(city.condition);
        iconLabel.setText(city.icon);
        windLabel.setText(city.wind + " km/h");
        humidityLabel.setText(city.humidity + "%");
        visibilityLabel.setText(city.visibility + " km");
        aqiLabel.setText(String.valueOf(city.aqi));
        uvLabel.setText(String.valueOf(city.uv));
        precipLabel.setText(city.precip + "%");
        if (mapCityLabel != null) {
            mapCityLabel.setText(city.name);
        }
        if (mapConditionLabel != null) {
            mapConditionLabel.setText(city.condition);
        }
        if (mapTempLabel != null) {
            mapTempLabel.setText(city.temperature + "°C");
        }
        recTitleLabel.setText("Places to Visit in " + city.name);

        if (city.alert == null || city.alert.isBlank()) {
            alertBanner.setManaged(false);
            alertBanner.setVisible(false);
        } else {
            alertBanner.setText("⚠  " + city.alert);
            alertBanner.setManaged(true);
            alertBanner.setVisible(true);
        }

        boolean shouldGoIndoor = city.precip > 30 || city.humidity > 78 || city.uv > 8;
        recSubtitleLabel.setText(shouldGoIndoor ? "⚠ Weather suggests indoor activities today" : "Based on current weather conditions");
        recSubtitleLabel.setTextFill(Color.web(shouldGoIndoor ? AMBER : TEXT_MUTED));
        if (weatherTagLabel != null) {
            weatherTagLabel.setText(city.humidity > 75 ? "Wet Weather" : city.uv > 8 ? "Hot Day" : "Good Weather");
        }
        updateTabStyles();

        buildCityList();
        buildForecast(city);
        buildChart(city);
        buildPlaces(city);
    }

    private void mergeBackendWeather(DashboardCity city) {
        if (city.liveWeatherLoaded || city.liveWeatherLoading) {
            return;
        }
        city.liveWeatherLoading = true;
        Thread worker = new Thread(() -> {
            try {
                WeatherModule module = getWeatherModule();
                if (module == null) {
                    return;
                }
                WeatherData data = module.fetchWeather(city.name);
                List<ForecastData> forecastData = module.fetchForecast(city.name);

                if (data != null) {
                    city.condition = data.getCondition();
                    city.temperature = (int) Math.round(data.getTemperature());
                    city.humidity = (int) Math.round(data.getHumidity());
                    city.wind = (int) Math.round(data.getWindSpeed());
                }

                if (forecastData != null && !forecastData.isEmpty()) {
                    city.forecast = toForecastDays(forecastData);
                    updateChartData(city);
                }

                Platform.runLater(() -> {
                    if (city.name.equals(cities.get(activeCityKey).name)) {
                        conditionLabel.setText(city.condition);
                        tempLabel.setText(city.temperature + "°C");
                        humidityLabel.setText(city.humidity + "%");
                        windLabel.setText(city.wind + " km/h");
                        if (mapConditionLabel != null) {
                            mapConditionLabel.setText(city.condition);
                        }
                        if (mapTempLabel != null) {
                            mapTempLabel.setText(city.temperature + "°C");
                        }
                        buildForecast(city);
                        buildChart(city);
                    }
                    buildCityList();
                });
            } catch (Throwable ignored) {
                // Keep the dashboard usable when the live weather service is offline.
            } finally {
                city.liveWeatherLoaded = true;
                city.liveWeatherLoading = false;
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private List<ForecastDay> toForecastDays(List<ForecastData> forecastData) {
        List<ForecastDay> forecast = new ArrayList<>();
        for (ForecastData data : forecastData) {
            String[] iconAndCondition = forecastIcon(data.getCondition());
            forecast.add(new ForecastDay(
                    data.getDate(),
                    iconAndCondition[0],
                    iconAndCondition[1],
                    (int) Math.round(data.getMaxTemp()),
                    (int) Math.round(data.getMinTemp()),
                    data.getPrecipitationProbability()
            ));
        }
        return forecast;
    }

    private String[] forecastIcon(String condition) {
        String text = condition == null ? "" : condition.toLowerCase(Locale.ROOT);

        if (text.contains("clear")) return new String[]{"☀", "Clear"};
        if (text.contains("cloud")) return new String[]{"☁", "Cloudy"};
        if (text.contains("rain")) return new String[]{"🌧", "Rain"};
        if (text.contains("storm")) return new String[]{"⛈", "Storm"};
        if (text.contains("fair")) return new String[]{"🌤", "Fair"};

        return new String[]{"🌤", condition == null || condition.isBlank() ? "Fair" : condition};
    }

    private void updateChartData(DashboardCity city) {
        int count = Math.min(7, city.forecast.size());
        for (int i = 0; i < count; i++) {
            ForecastDay day = city.forecast.get(i);
            city.tempData[i] = (day.high + day.low) / 2;
            city.rainData[i] = day.rainChance;
        }
    }

    private void buildCityList() {
        cityListBox.getChildren().clear();
        cities.forEach((key, city) -> {
            if (!key.equals(activeCityKey)) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(14, 16, 14, 16));
                row.setStyle("-fx-background-color:" + BG_CARD + "; -fx-border-color:" + BORDER + "; -fx-background-radius: 8; -fx-border-radius: 8;");
                row.setOnMouseClicked(event -> refreshDashboard(key));
                Label icon = new Label(city.icon);
                icon.setFont(Font.font(22));
                VBox info = new VBox(1, primaryText(city.name), mutedText(city.condition));
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                Label temp = new Label(city.temperature + "°C");
                temp.setFont(Font.font("Georgia", 20));
                temp.setTextFill(Color.web(TEXT_PRIMARY));
                row.getChildren().addAll(icon, info, spacer, temp);
                cityListBox.getChildren().add(row);
            }
        });
    }

    private void buildForecast(DashboardCity city) {
        forecastRows.getChildren().clear();

        List<PlaceInfo> featuredPlaces = getFeaturedPlaces(city.name);
        HBox row = new HBox(12);
        row.setAlignment(Pos.TOP_LEFT);

        int count = Math.min(3, featuredPlaces.size());
        for (int i = 0; i < count; i++) {
            row.getChildren().add(featuredPlaceCard(featuredPlaces.get(i)));
        }

        if (row.getChildren().isEmpty()) {
            Label empty = mutedText("No featured places available for this city yet.");
            forecastRows.getChildren().add(empty);
        } else {
            forecastRows.getChildren().add(row);
        }
    }

    private List<PlaceInfo> getFeaturedPlaces(String cityName) {
        RecommendationModule module = getRecommendationModule();
        if (module == null) {
            return new ArrayList<>();
        }
        List<PlaceInfo> places = module.recommend(cityName);
        return places == null ? new ArrayList<>() : places;
    }

    private Node featuredPlaceCard(PlaceInfo place) {
        VBox card = new VBox(8);
        card.setPrefWidth(310);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color:" + BG_CARD_2 + "; -fx-border-color:" + BORDER + "; -fx-background-radius: 10; -fx-border-radius: 10;");

        ImageView imageView = placeImage(place.getImagePath(), 290, 135);
        Label name = primaryText(place.getName());
        Label type = mutedText(place.getPlaceType() + " • " + place.getCategory());
        Label reason = mutedText(place.getDescription());
        reason.setWrapText(true);
        Label badge = pill("Popularity " + place.getPopularity(), BLUE, "rgba(88,166,255,0.14)");

        card.getChildren().addAll(imageView, name, type, reason, badge);
        return card;
    }

    private ImageView placeImage(String imagePath, double width, double height) {
        Image image = null;
        if (imagePath != null && !imagePath.isBlank()) {
            try {
                image = new Image(getClass().getResourceAsStream(imagePath));
            } catch (Exception ignored) {
                image = null;
            }
        }

        ImageView imageView = image == null || image.isError() ? new ImageView() : new ImageView(image);
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        imageView.setPreserveRatio(false);
        imageView.setStyle("-fx-background-color:" + BG_ACCENT + ";");
        return imageView;
    }

    private void buildChart(DashboardCity city) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setTickLabelFill(Color.web(TEXT_MUTED));
        yAxis.setTickLabelFill(Color.web(TEXT_MUTED));

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);
        chart.setStyle("-fx-background-color: transparent;");
        chart.setPrefHeight(180);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        int[] data;
        switch (activeChartTab) {
            case "humidity":
                data = city.humidityData;
                break;
            case "rain":
                data = city.rainData;
                break;
            default:
                data = city.tempData;
                break;
        }
        String[] labels = chartDayLabels();
        for (int i = 0; i < labels.length; i++) {
            series.getData().add(new XYChart.Data<>(labels[i], data[i]));
        }
        chart.getData().add(series);
        chartHolder.getChildren().setAll(chart);
    }

    private String forecastDayLabel(int dayOffset) {
        if (dayOffset == 0) {
            return "Today";
        }
        if (dayOffset == 1) {
            return "Tomorrow";
        }
        return LocalDate.now().plusDays(dayOffset).format(DateTimeFormatter.ofPattern("EEE"));
    }

    private String[] chartDayLabels() {
        String[] labels = new String[7];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = LocalDate.now().plusDays(i).format(DateTimeFormatter.ofPattern("EEE"));
        }
        return labels;
    }

    private void buildPlaces(DashboardCity city) {
        placesGrid.getChildren().clear();
        if (topPicksBox != null) {
            topPicksBox.getChildren().clear();
        }

        List<PlaceCard> places = new ArrayList<>(activeRecommendationTab.equals("indoor") ? city.indoor : city.outdoor);
        if (activeRecommendationTab.equals("outdoor")) {
            List<PlaceCard> cachedPlaces = backendRecommendationCache.get(city.name);
            if (cachedPlaces != null && !cachedPlaces.isEmpty()) {
                places.clear();
                places.addAll(cachedPlaces);
            } else if (cachedPlaces == null) {
                backendRecommendationCache.put(city.name, new ArrayList<>());
                loadBackendPlaces(city.name);
            }
        }

        for (int i = 0; i < places.size(); i++) {
            PlaceCard place = places.get(i);
            placesGrid.add(placeTile(place), i % 2, i / 2);
            if (topPicksBox != null && i < 3) {
                topPicksBox.getChildren().add(compactPlace(place));
            }
        }
    }

    private void loadBackendPlaces(String cityName) {
        Thread worker = new Thread(() -> {
            List<PlaceCard> fetchedPlaces = new ArrayList<>();
            try {
                RecommendationModule module = getRecommendationModule();
                if (module == null) {
                    return;
                }
                List<PlaceInfo> backendPlaces = module.recommend(cityName);
                if (backendPlaces != null) {
                    for (PlaceInfo place : backendPlaces) {
                        fetchedPlaces.add(new PlaceCard(place.getName(), place.getCategory(), "📍", "Recommended", BLUE));
                    }
                }
            } catch (Throwable ignored) {
                // Static places remain visible if backend recommendations fail.
            }
            backendRecommendationCache.put(cityName, fetchedPlaces);
            Platform.runLater(() -> {
                DashboardCity activeCity = cities.get(activeCityKey);
                if (activeCity != null && activeCity.name.equals(cityName) && activeRecommendationTab.equals("outdoor")) {
                    buildPlaces(activeCity);
                }
            });
        });
        worker.setDaemon(true);
        worker.start();
    }

    private WeatherModule getWeatherModule() {
        if (weatherModule == null) {
            try {
                weatherModule = new WeatherModule();
            } catch (Throwable ignored) {
                return null;
            }
        }
        return weatherModule;
    }

    private RecommendationModule getRecommendationModule() {
        if (recModule == null) {
            try {
                recModule = new RecommendationModule();
            } catch (Throwable ignored) {
                return null;
            }
        }
        return recModule;
    }

    private DatabaseManager getDatabaseManager() {
        if (db == null) {
            try {
                db = new DatabaseManager();
            } catch (Throwable ignored) {
                return null;
            }
        }
        return db;
    }

    private Node placeTile(PlaceCard place) {
        HBox tile = new HBox(10);
        tile.setPadding(new Insets(12));
        tile.setAlignment(Pos.TOP_LEFT);
        tile.setStyle("-fx-background-color:" + BG_CARD_2 + "; -fx-border-color:" + BORDER + "; -fx-background-radius: 10; -fx-border-radius: 10;");
        VBox text = new VBox(3, primaryText(place.name), mutedText(place.type), pill(place.tag, place.color, place.color + "22"));
        tile.getChildren().addAll(new Label(place.icon), text);
        return tile;
    }

    private Node compactPlace(PlaceCard place) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(10, 0, 10, 0));
        row.setStyle("-fx-border-color: transparent transparent " + BORDER + " transparent;");
        VBox info = new VBox(1, primaryText(place.name), mutedText(place.type));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(new Label(place.icon), info, spacer, pill(place.tag, place.color, place.color + "22"));
        return row;
    }

    private Button navButton(String text, String tooltip, boolean active, Runnable action) {
        Button button = new Button(text);
        button.setPrefSize(44, 44);
        button.setTooltip(new Tooltip(tooltip));
        button.setOnAction(event -> {
            setActiveNavButton(button);
            action.run();
        });
        if (active) {
            activeNavButton = button;
        }
        styleNavButton(button, active);
        return button;
    }

    private void setActiveNavButton(Button button) {
        if (activeNavButton != null) {
            styleNavButton(activeNavButton, false);
        }
        activeNavButton = button;
        styleNavButton(button, true);
    }

    private void styleNavButton(Button button, boolean active) {
        button.setStyle("-fx-background-color:" + (active ? BG_ACCENT : "transparent") + "; -fx-background-radius: 8; -fx-text-fill:" + (active ? BLUE : TEXT_MUTED) + "; -fx-font-size: 17; -fx-font-weight: bold;");
    }

    private void scrollTo(double value) {
        if (dashboardScroll != null) {
            dashboardScroll.setVvalue(value);
        }
    }

    private void showAlertCity() {
        cities.entrySet().stream()
                .filter(entry -> entry.getValue().alert != null && !entry.getValue().alert.isBlank())
                .findFirst()
                .ifPresent(entry -> refreshDashboard(entry.getKey()));
        scrollTo(0);
    }

    private void searchFocusMessage() {
        if (recSubtitleLabel != null) {
            recSubtitleLabel.setText("Use search or the city cards to switch cities.");
            recSubtitleLabel.setTextFill(Color.web(BLUE));
        }
    }

    private Button iconButton(String text) {
        Button button = new Button(text);
        button.setPrefSize(36, 36);
        button.setStyle("-fx-background-color:" + BG_ACCENT + "; -fx-background-radius: 18; -fx-border-color:" + BORDER + "; -fx-border-radius: 18; -fx-text-fill:" + TEXT_SECONDARY + ";");
        return button;
    }

    private Button chartTab(String text, String key) {
        Button button = new Button(text);
        button.setStyle(tabStyle(key.equals(activeChartTab)));
        chartButtons.put(key, button);
        button.setOnAction(event -> {
            activeChartTab = key;
            updateTabStyles();
            buildChart(cities.get(activeCityKey));
        });
        return button;
    }

    private Button recommendationTab(String text, String key) {
        Button button = new Button(text);
        button.setStyle(tabStyle(key.equals(activeRecommendationTab)));
        recommendationButtons.put(key, button);
        button.setOnAction(event -> {
            activeRecommendationTab = key;
            updateTabStyles();
            buildPlaces(cities.get(activeCityKey));
        });
        return button;
    }

    private void updateTabStyles() {
        chartButtons.forEach((key, button) -> button.setStyle(tabStyle(key.equals(activeChartTab))));
        recommendationButtons.forEach((key, button) -> button.setStyle(tabStyle(key.equals(activeRecommendationTab))));
    }

    private VBox card() {
        VBox card = new VBox(16);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color:" + BG_CARD + "; -fx-border-color:" + BORDER + "; -fx-background-radius: 14; -fx-border-radius: 14;");
        return card;
    }

    private HBox header(String title, String action) {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = primaryText(title);
        Label actionLabel = new Label(action);
        actionLabel.setTextFill(Color.web(BLUE));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(titleLabel, spacer, actionLabel);
        return header;
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text.toUpperCase(Locale.ROOT));
        label.setTextFill(Color.web(TEXT_MUTED));
        label.setFont(Font.font(11));
        label.setPadding(new Insets(0, 4, 0, 4));
        return label;
    }

    private VBox metricTile(String label, Label value, String sub) {
        VBox tile = new VBox(4);
        tile.setPadding(new Insets(12));
        tile.setStyle("-fx-background-color:" + BG_CARD + "; -fx-border-color:" + BORDER + "; -fx-background-radius: 8; -fx-border-radius: 8;");
        Label labelNode = sectionLabel(label);
        Label subNode = mutedText(sub);
        Rectangle bar = new Rectangle(54, 4, Color.web(BG_ACCENT));
        bar.setArcHeight(4);
        bar.setArcWidth(4);
        tile.getChildren().addAll(labelNode, value, subNode, bar);
        return tile;
    }

    private VBox stat(String label, Label value) {
        VBox stat = new VBox(2);
        Label labelNode = sectionLabel(label);
        stat.getChildren().addAll(labelNode, value);
        return stat;
    }

    private Label primaryText(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web(TEXT_PRIMARY));
        label.setFont(Font.font(13));
        return label;
    }

    private Label mutedText(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web(TEXT_MUTED));
        label.setFont(Font.font(12));
        return label;
    }

    private Label smallValue() {
        Label label = new Label();
        label.setTextFill(Color.web("#dce7f3"));
        label.setFont(Font.font(13));
        return label;
    }

    private Label valueLabel(String color) {
        Label label = new Label();
        label.setTextFill(Color.web(color));
        label.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, 20));
        return label;
    }

    private Label fixedText(String text, double width, String color) {
        Label label = new Label(text);
        label.setPrefWidth(width);
        label.setTextFill(Color.web(color));
        label.setFont(Font.font(13));
        return label;
    }

    private Label pill(String text, String color, String background) {
        Label label = new Label(text);
        label.setTextFill(Color.web(color));
        label.setPadding(new Insets(3, 8, 3, 8));
        label.setStyle("-fx-background-color:" + background + "; -fx-background-radius: 10; -fx-font-size: 10;");
        return label;
    }

    private Label avatar(String initials, int size) {
        Label avatar = new Label(initials);
        avatar.setAlignment(Pos.CENTER);
        avatar.setPrefSize(size, size);
        avatar.setTextFill(Color.WHITE);
        avatar.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, 12));
        avatar.setStyle("-fx-background-color: linear-gradient(to bottom right, #3fb950, #58a6ff); -fx-background-radius:" + size / 2 + ";");
        return avatar;
    }

    private Node pin(String name, String color, double x, double y) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        Circle dot = new Circle(5, Color.web(color));
        Label label = mutedText(name);
        box.getChildren().addAll(dot, label);
        StackPane.setMargin(box, new Insets(y, 0, 0, x));
        return box;
    }

    private Node mapLine(double startX, double startY, double endX, double endY) {
        Line line = new Line(startX, startY, endX, endY);
        line.setStroke(Color.web(BLUE, 0.35));
        line.getStrokeDashArray().addAll(4.0, 4.0);
        return line;
    }

    private String inputStyle() {
        return "-fx-background-color:" + BG_ACCENT + "; -fx-background-radius: 24; -fx-border-color:" + BORDER + "; -fx-border-radius: 24; -fx-text-fill:" + TEXT_PRIMARY + "; -fx-prompt-text-fill:" + TEXT_MUTED + "; -fx-padding: 9 14;";
    }

    private String tabStyle(boolean active) {
        return "-fx-background-color:" + (active ? BLUE : "transparent") + "; -fx-border-color:" + (active ? BLUE : BORDER) + "; -fx-background-radius: 20; -fx-border-radius: 20; -fx-text-fill:" + (active ? "#ffffff" : TEXT_MUTED) + "; -fx-font-size: 12;";
    }

    private Map<String, DashboardCity> buildCities() {
        Map<String, DashboardCity> map = new LinkedHashMap<>();
        map.put("dehradun", new DashboardCity("Dehradun", "🌤", 24, "Partly Cloudy", 58, 12, 14, 72, 6, 12, null,
                days(new ForecastDay("Mon", "💨", "Windy", 23, 18), new ForecastDay("Tue", "⛈", "Storm", 18, 15), new ForecastDay("Wed", "🌧", "Rainy", 24, 19), new ForecastDay("Thu", "☀", "Sunny", 28, 22), new ForecastDay("Fri", "⛅", "Partly Cloudy", 26, 20), new ForecastDay("Sat", "🌤", "Mostly Clear", 27, 21), new ForecastDay("Sun", "🌦", "Showers", 22, 17)),
                new int[]{19, 18, 20, 24, 26, 25, 22}, new int[]{60, 65, 70, 58, 52, 55, 63}, new int[]{2, 8, 15, 3, 0, 0, 10},
                places(new PlaceCard("Robber's Cave", "Scenic Cave & Stream", "🏞", "Must Visit", GREEN), new PlaceCard("Sahastradhara", "Sulphur Springs", "🌊", "Popular", BLUE), new PlaceCard("Mindrolling Monastery", "Spiritual Site", "🛕", "Cultural", PURPLE), new PlaceCard("Forest Research Inst.", "Heritage & Gardens", "🌳", "Family", GREEN)),
                places(new PlaceCard("Wadia Institute", "Himalayan Geology Museum", "🔬", "Educational", BLUE), new PlaceCard("Clock Tower Market", "Shopping & Street Food", "🛍", "Foodie", AMBER), new PlaceCard("Pacific Mall", "Shopping Complex", "🏬", "Indoor", AMBER), new PlaceCard("Regional Science Centre", "Interactive Museum", "🚀", "Fun", PURPLE))));

        map.put("mussoorie", new DashboardCity("Mussoorie", "🌫", 14, "Foggy & Cool", 82, 18, 4, 38, 3, 45, "Low visibility due to fog. Drive carefully on mountain roads.",
                days(new ForecastDay("Mon", "🌫", "Fog", 14, 9), new ForecastDay("Tue", "🌧", "Drizzle", 12, 8), new ForecastDay("Wed", "🌦", "Showers", 13, 9), new ForecastDay("Thu", "🌤", "Partly Clear", 16, 10), new ForecastDay("Fri", "☀", "Clear", 18, 11), new ForecastDay("Sat", "⛅", "Cloudy", 15, 9), new ForecastDay("Sun", "🌧", "Rain", 11, 7)),
                new int[]{10, 9, 11, 14, 16, 15, 12}, new int[]{85, 90, 88, 82, 75, 78, 86}, new int[]{5, 15, 20, 8, 0, 3, 18},
                places(new PlaceCard("Mall Road", "Iconic Promenade", "🚶", "Overcast", AMBER), new PlaceCard("Kempty Falls", "Waterfall & Picnic", "💦", "Scenic", BLUE), new PlaceCard("Gun Hill", "Cable Car & Views", "🚡", "Views", PURPLE), new PlaceCard("Lal Tibba", "Highest Peak in Mussoorie", "⛰", "Trekking", GREEN)),
                places(new PlaceCard("Landour Bakehouse", "Cozy Cafe & Bakery", "☕", "Warm Up", AMBER), new PlaceCard("Tibetan Market", "Handicraft Shopping", "🎨", "Shopping", AMBER), new PlaceCard("Cloud End Resort", "Heritage Hotel & SPA", "🏨", "Relaxing", BLUE), new PlaceCard("Sisters Bazaar", "Antique Lane & Cafes", "🏺", "Quaint", PURPLE))));

        map.put("haridwar", new DashboardCity("Haridwar", "☀", 30, "Hot & Sunny", 42, 8, 20, 95, 9, 5, "UV Index is very high. Use sunscreen and stay hydrated.",
                days(new ForecastDay("Mon", "☀", "Sunny", 30, 22), new ForecastDay("Tue", "☀", "Clear", 31, 23), new ForecastDay("Wed", "🌤", "Partly Cloudy", 29, 21), new ForecastDay("Thu", "⛅", "Cloudy", 27, 20), new ForecastDay("Fri", "🌦", "Light Shower", 25, 19), new ForecastDay("Sat", "🌧", "Rain", 22, 17), new ForecastDay("Sun", "☀", "Sunny", 28, 20)),
                new int[]{24, 25, 30, 30, 29, 28, 26}, new int[]{40, 38, 42, 45, 50, 55, 43}, new int[]{0, 0, 0, 2, 5, 12, 0},
                places(new PlaceCard("Har Ki Pauri", "Sacred Ghat & Aarti", "🪔", "Must Visit", AMBER), new PlaceCard("Chandi Devi Temple", "Cable Car Temple", "🛕", "Spiritual", PURPLE), new PlaceCard("Rajaji National Park", "Wildlife Safari", "🐘", "Adventure", GREEN), new PlaceCard("Mansa Devi Temple", "Hilltop Shrine", "⛩", "Views", AMBER)),
                places(new PlaceCard("ISKCON Temple", "Spiritual Complex", "🙏", "Cool Inside", BLUE), new PlaceCard("Patanjali Yog Peeth", "Yoga & Wellness", "🧘", "Wellness", GREEN), new PlaceCard("Birla Ganga Heritage", "Museum & Culture", "🏛", "Cultural", PURPLE), new PlaceCard("Anandamayi Ashram", "Meditation & Rest", "🕯", "Peaceful", BLUE))));

        map.put("rishikesh", new DashboardCity("Rishikesh", "⛅", 27, "Warm & Breezy", 52, 14, 16, 55, 7, 18, null,
                days(new ForecastDay("Mon", "⛅", "Partly Cloudy", 27, 20), new ForecastDay("Tue", "🌤", "Mostly Clear", 28, 21), new ForecastDay("Wed", "☀", "Sunny", 29, 22), new ForecastDay("Thu", "🌦", "Light Rain", 24, 18), new ForecastDay("Fri", "🌧", "Rainy", 21, 16), new ForecastDay("Sat", "🌦", "Showers", 23, 17), new ForecastDay("Sun", "⛅", "Cloudy", 25, 19)),
                new int[]{21, 22, 27, 27, 28, 26, 24}, new int[]{55, 52, 50, 55, 65, 70, 58}, new int[]{0, 0, 0, 8, 20, 15, 3},
                places(new PlaceCard("Laxman Jhula", "Iconic Suspension Bridge", "🌉", "Iconic", GREEN), new PlaceCard("White Water Rafting", "Rafting Class III/IV", "🚣", "Thrilling", RED), new PlaceCard("Neer Garh Waterfall", "Trekking & Swimming", "🌊", "Refreshing", BLUE), new PlaceCard("Beatles Ashram", "Meditation Camp", "🎸", "Unique", PURPLE)),
                places(new PlaceCard("Triveni Ghat", "Evening Aarti Ceremony", "🪔", "Spiritual", AMBER), new PlaceCard("Parmarth Niketan", "Yoga & Meditation Retreat", "🧘", "Wellness", GREEN), new PlaceCard("The Great Cafe", "Riverside Cafe & Music", "🎵", "Chill", BLUE), new PlaceCard("Sivananda Ashram", "Yoga Classes & Rest", "🛌", "Peaceful", PURPLE))));
        return map;
    }

    private List<ForecastDay> days(ForecastDay... days) {
        return Arrays.asList(days);
    }

    private List<PlaceCard> places(PlaceCard... places) {
        return Arrays.asList(places);
    }

    private static class DashboardCity {
        private final String name;
        private final String icon;
        private int temperature;
        private String condition;
        private int humidity;
        private boolean liveWeatherLoaded;
        private boolean liveWeatherLoading;
        private int wind;
        private final int visibility;
        private final int aqi;
        private final int uv;
        private final int precip;
        private final String alert;
        private List<ForecastDay> forecast;
        private final int[] tempData;
        private final int[] humidityData;
        private final int[] rainData;
        private final List<PlaceCard> outdoor;
        private final List<PlaceCard> indoor;

        private DashboardCity(String name, String icon, int temperature, String condition, int humidity, int wind, int visibility, int aqi, int uv, int precip, String alert, List<ForecastDay> forecast, int[] tempData, int[] humidityData, int[] rainData, List<PlaceCard> outdoor, List<PlaceCard> indoor) {
            this.name = name;
            this.icon = icon;
            this.temperature = temperature;
            this.condition = condition;
            this.humidity = humidity;
            this.wind = wind;
            this.visibility = visibility;
            this.aqi = aqi;
            this.uv = uv;
            this.precip = precip;
            this.alert = alert;
            this.forecast = forecast;
            this.tempData = tempData;
            this.humidityData = humidityData;
            this.rainData = rainData;
            this.outdoor = outdoor;
            this.indoor = indoor;
        }
    }

    private static class ForecastDay {
        private final String day;
        private final String icon;
        private final String condition;
        private final int high;
        private final int low;
        private final int rainChance;

        private ForecastDay(String day, String icon, String condition, int high, int low) {
            this(day, icon, condition, high, low, 0);
        }

        private ForecastDay(String day, String icon, String condition, int high, int low, int rainChance) {
            this.day = day;
            this.icon = icon;
            this.condition = condition;
            this.high = high;
            this.low = low;
            this.rainChance = rainChance;
        }
    }

    private static class PlaceCard {
        private final String name;
        private final String type;
        private final String icon;
        private final String tag;
        private final String color;

        private PlaceCard(String name, String type, String icon, String tag, String color) {
            this.name = name;
            this.type = type;
            this.icon = icon;
            this.tag = tag;
            this.color = color;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
