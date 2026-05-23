package shaleva.run_app_proj.datamodels;

public class RunDataSummary {
    private double totalDistanceKm;
    private long durationMillis;
    private double averagePace;
    private double totalElevationGain;
    private int totalSteps;

    
    public RunDataSummary(double totalDistanceKm, long durationMillis, double averagePace, double totalElevationGain,
            int totalSteps) {
        this.totalDistanceKm = totalDistanceKm;
        this.durationMillis = durationMillis;
        this.averagePace = averagePace;
        this.totalElevationGain = totalElevationGain;
        this.totalSteps = totalSteps;
    }

    public RunDataSummary() {}

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }
    public void setTotalDistanceKm(double totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }
    public long getDurationMillis() {
        return durationMillis;
    }
    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }
    public double getAveragePace() {
        return averagePace;
    }
    public void setAveragePace(double averagePace) {
        this.averagePace = averagePace;
    }
    public double getTotalElevationGain() {
        return totalElevationGain;
    }
    public void setTotalElevationGain(double totalElevationGain) {
        this.totalElevationGain = totalElevationGain;
    }
    public int getTotalSteps() {
        return totalSteps;
    }
    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    
}
