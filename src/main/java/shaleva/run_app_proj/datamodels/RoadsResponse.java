package shaleva.run_app_proj.datamodels;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RoadsResponse {
    private List<SnappedPoint> snappedPoints;

    public List<SnappedPoint> getSnappedPoints() { return snappedPoints; }
    public void setSnappedPoints(List<SnappedPoint> snappedPoints) { this.snappedPoints = snappedPoints; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SnappedPoint {
        private Location location;
        private Integer originalIndex;
        private String placeId;

        public Location getLocation() { return location; }
        public void setLocation(Location location) { this.location = location; }
        public Integer getOriginalIndex() { return originalIndex; }
        public void setOriginalIndex(Integer originalIndex) { this.originalIndex = originalIndex; }
        public String getPlaceId() { return placeId; }
        public void setPlaceId(String placeId) { this.placeId = placeId; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        private double latitude;
        private double longitude;

        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
    }
}