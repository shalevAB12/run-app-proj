package shaleva.run_app_proj.datamodels;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RouteRequestObject {
    @JsonProperty("latitude")
    private double startLat;

    @JsonProperty("longitude")
    private double startLng;

    @JsonProperty("maxLength")
    private double distance; // המרחק המבוקש במטרים או ק"מ


    public RouteRequestObject(double startLat, double startLng, double distance) {
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

}
