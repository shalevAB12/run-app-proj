package shaleva.run_app_proj.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import shaleva.run_app_proj.datamodels.RouteRequest;
import shaleva.run_app_proj.datamodels.Waypoint;
import shaleva.run_app_proj.services.RouteService;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    @Autowired
    private RouteService routeService;

    @PostMapping("/generate")
    public ResponseEntity<List<Waypoint>> generateRoute(@RequestBody RouteRequest request) {
        List<Waypoint> route = routeService.calculateOptimizedRoute(request);
        return ResponseEntity.ok(route);
    }

}
