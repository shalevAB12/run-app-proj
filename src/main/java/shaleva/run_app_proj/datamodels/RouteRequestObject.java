package shaleva.run_app_proj.datamodels;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RouteRequestObject {
    @JsonProperty("latitude")
    private double startLat;

    @JsonProperty("longitude")
    private double startLng;

    @JsonProperty("maxLength")
    private double distance; 

    @JsonProperty("isCircular")
    private boolean isCircular;

    @JsonProperty("selectedCategories")
    private List<String> selectedCategories;

    public RouteRequestObject() {}

    public RouteRequestObject(double startLat, double startLng, double distance, boolean isCircular, ArrayList<String> selectedCategories) {
        this.startLat = startLat;
        this.startLng = startLng;
        this.distance = distance;
        this.isCircular = isCircular;
    }

    public double getStartLat() { return startLat; }

    public double getStartLng() { return startLng; }

    public double getDistance() { return distance; }

    public List<String> getSelectedCategories() {
        return selectedCategories;
    }

    public boolean isCircular() {
        return isCircular;
    }

}
