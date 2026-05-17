package shaleva.run_app_proj.datamodels;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Waypoint {
    @JsonProperty("lat")
    private double lat;

    @JsonProperty("lng")
    private double lng;

    @JsonProperty("placeId")
    private String placeId;

    @JsonProperty("reward")
    private double reward;

    @JsonProperty("distanceToNext")
    private double distanceToNext;

    public Waypoint() {}

    public Waypoint(double lat, double lng, String placeId, double reward, double distanceToNext) {
        this.lat = lat;
        this.lng = lng;
        this.placeId = placeId;
        this.reward = reward;
        this.distanceToNext = distanceToNext;
    }

    // Getters & Setters
    public double getLat() { return lat; }
    public double getLng() { return lng; }
    public String getPlaceId() { return placeId; }
    public double getReward() { return reward; }
    public double getDistanceToNext() { return distanceToNext; }

}
