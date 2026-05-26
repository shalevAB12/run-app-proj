package shaleva.run_app_proj.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import shaleva.run_app_proj.datamodels.RunDataPoint;
import shaleva.run_app_proj.datamodels.RunDataSummary;
import shaleva.run_app_proj.datamodels.RunSession;
import shaleva.run_app_proj.datamodels.RunStatus;
import shaleva.run_app_proj.repositories.RunSessionRepository;

import java.util.List;

@Service
public class RunStatisticsService {

    // מאגר הנתונים (למשל MongoDB)
    @Autowired
    private RunSessionRepository runRepository;

    public String startRunSession(String userId) {
        RunSession newSession = new RunSession();
        newSession.setUserId(userId);
        newSession.setStatus(RunStatus.ACTIVE);
        newSession.setStartTime(System.currentTimeMillis()); // כדאי להוסיף שדה כזה

        // שמירה והחזרת ה-ID שהמונגו ייצר
        RunSession saved = runRepository.insert(newSession);
        return saved.getRunId();
    }

    public void addDataPoints(String runId, List<RunDataPoint> newPoints) {
        RunSession session = runRepository.findById(runId)
                .orElseThrow(() -> new RuntimeException("Run not found. Did you call startRun?"));

        session.getDataPoints().addAll(newPoints);
        runRepository.save(session);
    }

    /**
     * פונקציה זו נקראת כשהאנדרואיד מדווח על סיום הריצה
     */
    public RunSession finalizeRunStatistics(String runId, long pauseDurationMillis) {
        RunSession session = runRepository.findById(runId)
                .orElseThrow(() -> new RuntimeException("Run session not found"));

        List<RunDataPoint> points = session.getDataPoints();

        RunDataSummary summary = new RunDataSummary();

        if (points == null || points.isEmpty()) {
            session.setSummary(summary); 
        } else {
            double totalDistanceMeters = 0;
            double totalElevationGain = 0;
            int totalSteps = 0;

            for (int i = 1; i < points.size(); i++) {
                RunDataPoint prev = points.get(i - 1);
                RunDataPoint curr = points.get(i);

                if (curr.isFirstPoint())
                    continue;
                if (curr.getAccuracy() < 20.0) {
                    totalDistanceMeters += calculateDistance(
                            prev.getLatitude(), prev.getLongitude(),
                            curr.getLatitude(), curr.getLongitude());
                }

                double elevationDiff = curr.getAltitude() - prev.getAltitude();
                if (elevationDiff > 0.5) { // Hysteresis: התעלמות מקפיצות קטנות ורעשים
                    totalElevationGain += elevationDiff;
                }

                totalSteps += curr.getStepDelta();
            }

            long endTime = System.currentTimeMillis();

            long grossDuration = endTime - session.getStartTime();

            long activeDurationMillis = grossDuration - pauseDurationMillis;

            double totalDistanceKm = totalDistanceMeters / 1000.0;
            double averagePace = 0;
            if (totalDistanceKm > 0) {
                double durationMinutes = activeDurationMillis / 60000.0;
                averagePace = durationMinutes / totalDistanceKm;
            }

            
            summary.setTotalDistanceKm(totalDistanceKm);
            summary.setDurationMillis(activeDurationMillis); // במקום totalDurationMillis
            summary.setAveragePace(averagePace);
            summary.setTotalElevationGain(totalElevationGain);
            summary.setTotalSteps(totalSteps);
        }

        session.setSummary(summary);
        session.setStatus(RunStatus.FINISHED);
        runRepository.save(session);

        return session;
    }

    /**
     * פונקציית עזר לחישוב מרחק בין שתי קואורדינטות בשיטת Haversine
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // רדיוס כדור הארץ בקילומטרים
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000; // המרה למטרים
    }

    // בתוך RunStatisticsService.java
    public List<RunSession> getUserHistory(String userId) {
        return runRepository.findByUserIdOrderByStartTimeDesc(userId);
    }
}