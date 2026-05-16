package shaleva.run_app_proj.datamodels;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Candidate {
    private String id;
    private double lat;
    private double lng;
    private double reward;

    public Candidate(String id, double lat, double lng, double reward) {
        this.id = id;
        this.lat = lat;
        this.lng = lng;
        this.reward = reward;
    }

    @JsonProperty("id") public String getId() { return id; }
    @JsonProperty("lat") public double getLat() { return lat; }
    @JsonProperty("lng") public double getLng() { return lng; }
    @JsonProperty("reward") public double getReward() { return reward; }

    // Setters (נדרשים אם תרצה לשמור ב-MongoDB)
    public void setReward(double reward) { this.reward = reward; }

    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // בדיקה אם זה אותו אובייקט בדיוק בזיכרון
        if (!(o instanceof Candidate)) return false; // בדיקה אם זה בכלל Candidate
        Candidate candidate = (Candidate) o;
        return id.equals(candidate.id); // השוואה רק לפי ה-ID
    }

    @Override
    public int hashCode() {
        return id.hashCode(); // ה-Hash של האובייקט הוא ה-Hash של ה-ID שלו
    }
}