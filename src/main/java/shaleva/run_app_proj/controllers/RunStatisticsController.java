package shaleva.run_app_proj.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestBody;
import shaleva.run_app_proj.datamodels.RunDataPoint;
import shaleva.run_app_proj.datamodels.RunDataSummary;
import shaleva.run_app_proj.datamodels.RunPointsUpdateRequestObject;
import shaleva.run_app_proj.datamodels.RunSession;
import shaleva.run_app_proj.services.RunStatisticsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class RunStatisticsController {
    @Autowired
    private RunStatisticsService runService;

    // 1. התחלת ריצה
    @PostMapping("/start")
    public ResponseEntity<String> startRun(@RequestParam String userId) {
        String runId = runService.startRunSession(userId);
        return ResponseEntity.ok(runId);
    }

    // 2. עדכון נקודות - ה-ID נמצא בתוך ה-DTO
    @PostMapping("/update_points")
    public ResponseEntity<Void> addPoints(@RequestBody RunPointsUpdateRequestObject request) {
        runService.addDataPoints(request.getRunId(), request.getPoints());
        return ResponseEntity.noContent().build();
    }

    // 3. סיום ריצה - קבלת ה-ID כפרמטר (נראה ככה: /api/runs/finish?runId=123)
    // או לחלופין בתוך אובייקט Body קטן
    @PostMapping("/finish")
    public ResponseEntity<RunSession> finishRun(@RequestParam String runId, @RequestParam long pauseDurationMillis) {
        RunSession summarizedRunSession = runService.finalizeRunStatistics(runId, pauseDurationMillis);
        return ResponseEntity.ok(summarizedRunSession);
    }

    @PostMapping("/history")
    public ResponseEntity<List<RunSession>> getUserHistory(@RequestParam String userId) {
        try {
            List<RunSession> history = runService.getUserHistory(userId);
            
            if (history.isEmpty()) {
                return ResponseEntity.noContent().build(); // מחזיר 204 אם אין ריצות
            }
            
            return ResponseEntity.ok(history); // מחזיר 200 עם הרשימה
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
