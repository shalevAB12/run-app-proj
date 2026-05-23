package shaleva.run_app_proj.datamodels;

public class RunDataPoint {
    private double latitude;
    private double longitude;
    private double altitude;
    private float speed;       // המהירות שהאנדרואיד מדד באותו רגע
    private long timestamp;    // זמן מדויק של הדגימה
    private float accuracy;    // רמת דיוק ה-GPS בנקודה זו
    private int stepDelta;
    private boolean isFirstPoint;

    public RunDataPoint() {}

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }

    public int getStepDelta() {
        return stepDelta;
    }

    public void setStepDelta(int stepDelta) {
        this.stepDelta = stepDelta;
    }

    public boolean isFirstPoint() {
        return isFirstPoint;
    }

    public void setFirstPoint(boolean isFirstPoint) {
        this.isFirstPoint = isFirstPoint;
    }

    
}
