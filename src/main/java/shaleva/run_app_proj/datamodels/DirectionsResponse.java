package shaleva.run_app_proj.datamodels;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DirectionsResponse {

    private String status;
    private List<Route> routes;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<Route> getRoutes() { return routes; }
    public void setRoutes(List<Route> routes) { this.routes = routes; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Route {
        private OverviewPolyline overview_polyline;
        private List<Leg> legs;

        public OverviewPolyline getOverview_polyline() { return overview_polyline; }
        public void setOverview_polyline(OverviewPolyline overview_polyline) { this.overview_polyline = overview_polyline; }

        public List<Leg> getLegs() { return legs; }
        public void setLegs(List<Leg> legs) { this.legs = legs; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OverviewPolyline {
        private String points; // זוהי המחרוזת המקודדת (Encoded Polyline) שאנדרואיד צריך

        public String getPoints() { return points; }
        public void setPoints(String points) { this.points = points; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Leg {
        private TextValue distance;
        private TextValue duration;

        public TextValue getDistance() { return distance; }
        public void setDistance(TextValue distance) { this.distance = distance; }

        public TextValue getDuration() { return duration; }
        public void setDuration(TextValue duration) { this.duration = duration; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TextValue {
        private String text;
        private int value;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
    }
}
