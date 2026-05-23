package shaleva.run_app_proj.datamodels;

import java.util.List;

public class RunPointsUpdateRequestObject {
    private String runId;
    private List<RunDataPoint> points;

    // Getters and Setters
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    public List<RunDataPoint> getPoints() { return points; }
    public void setPoints(List<RunDataPoint> points) { this.points = points; }
}
