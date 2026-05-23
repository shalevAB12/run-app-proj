package shaleva.run_app_proj.datamodels;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "Runs") // שם הקולקשן ב-MongoDB
public class RunSession {
    @Id
    private String runId;
    private String userId;
    private RunStatus status; // Enum: ACTIVE, FINISHED
    private long startTime;
    
    // רשימת הנקודות נשמרת בתוך המסמך
    private List<RunDataPoint> dataPoints = new ArrayList<>();
    
    // סיכום הסטטיסטיקות שמתמלא בסוף
    private RunDataSummary summary;

    public RunSession() {}

    public RunSession(String id, String userId, RunStatus status, List<RunDataPoint> dataPoints,
            RunDataSummary summary) {
        this.runId = id;
        this.userId = userId;
        this.status = status;
        this.dataPoints = dataPoints;
        this.summary = summary;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String id) {
        this.runId = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public RunStatus getStatus() {
        return status;
    }

    public void setStatus(RunStatus status) {
        this.status = status;
    }

    public List<RunDataPoint> getDataPoints() {
        return dataPoints;
    }

    public void setDataPoints(List<RunDataPoint> dataPoints) {
        this.dataPoints = dataPoints;
    }

    public RunDataSummary getSummary() {
        return summary;
    }

    public void setSummary(RunDataSummary summary) {
        this.summary = summary;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    
    
}
