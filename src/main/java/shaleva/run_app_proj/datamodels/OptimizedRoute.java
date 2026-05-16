package shaleva.run_app_proj.datamodels;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OptimizedRoute {

    @JsonProperty
    private double totalDistance;

    @JsonProperty
    private double totalReward;

    @JsonProperty
    private List<Waypoint> path;

    public OptimizedRoute() {}

    public OptimizedRoute(List<Waypoint> path) {
        this.path = path;
        calculateTotalReward();
        calculateTotalDistance();
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public double getTotalReward() {
        return totalReward;
    }

    public List<Waypoint> getPath() {
        return path;
    }

    @JsonIgnore
    public void calculateTotalReward() {
        double inTotal = 0;
        for (Waypoint waypoint : path) {
            inTotal += waypoint.getReward();
        }

        this.totalReward = inTotal;
    }
    
    @JsonIgnore
    public void calculateTotalDistance() {
        double inTotal = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            inTotal += path.get(i).getDistanceToNext();
        }

        this.totalDistance = inTotal;
    }
    
}