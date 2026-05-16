package shaleva.run_app_proj.datamodels;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacesResponse {

    @JsonProperty("places")
    private List<Place> places;

    public List<Place> getPlaces() { return places; }
    public void setPlaces(List<Place> places) { this.places = places; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Place {
        private String id;
        private List<String> types;
        private Double rating;
        private Integer userRatingCount;
        private Location location;
        private DisplayName displayName;

        @JsonProperty("id") public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        @JsonProperty("types") public List<String> getTypes() { return types; }
        public void setTypes(List<String> types) { this.types = types; }

        @JsonProperty("rating") public Double getRating() { return rating; }
        public void setRating(Double rating) { this.rating = rating; }

        @JsonProperty("userRatingCount") public Integer getUserRatingCount() { return userRatingCount; }
        public void setUserRatingCount(Integer userRatingCount) { this.userRatingCount = userRatingCount; }

        @JsonProperty("location") public Location getLocation() { return location; }
        public void setLocation(Location location) { this.location = location; }

        @JsonProperty("displayName") public DisplayName getDisplayName() { return displayName; }
        public void setDisplayName(DisplayName displayName) { this.displayName = displayName; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        private double latitude;
        private double longitude;

        @JsonProperty("latitude") public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }

        @JsonProperty("longitude") public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DisplayName {
        private String text;

        @JsonProperty("text") public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }
}
