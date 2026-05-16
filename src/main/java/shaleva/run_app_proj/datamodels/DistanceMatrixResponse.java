package shaleva.run_app_proj.datamodels;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DistanceMatrixResponse {

    private List<Row> rows;
    private String status;

    public List<Row> getRows() { return rows; }
    public void setRows(List<Row> rows) { this.rows = rows; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Row {
        private List<Element> elements;
        
        public List<Element> getElements() { return elements; }
        public void setElements(List<Element> elements) { this.elements = elements; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Element {
        private Distance distance;
        private String status;

        public Distance getDistance() { return distance; }
        public void setDistance(Distance distance) { this.distance = distance; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Distance {
        private int value; // הערך חוזר במטרים! מושלם בשבילנו

        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
    }
}