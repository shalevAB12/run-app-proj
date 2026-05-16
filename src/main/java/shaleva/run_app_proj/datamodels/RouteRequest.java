package shaleva.run_app_proj.datamodels;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RouteRequest {
    @JsonProperty("startLat")
    private double startLat;

    @JsonProperty("startLng")
    private double startLng;

    @JsonProperty("distance")
    private double distance; // המרחק המבוקש במטרים או ק"מ

    @JsonProperty("userId")
    private String userId;

    public RouteRequest(double startLat, double startLng, double distance) {
        this.startLat = startLat;
        this.startLng = startLng;
        this.distance = distance;
    }

    // Getters & Setters (חובה עבור Jackson)
    public double getStartLat() { return startLat; }
    public void setStartLat(double startLat) { this.startLat = startLat; }

    public double getStartLng() { return startLng; }
    public void setStartLng(double startLng) { this.startLng = startLng; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
