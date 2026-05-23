package shaleva.run_app_proj.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import shaleva.run_app_proj.datamodels.OptimizedRoute;
import shaleva.run_app_proj.datamodels.RouteRequestObject;
import shaleva.run_app_proj.services.RouteService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class RouteController {

    @Autowired
    private RouteService routeService;

    @PostMapping("/generate_route")
    public ResponseEntity<OptimizedRoute> generateRoute(@RequestBody RouteRequestObject request) {
        OptimizedRoute route = routeService.calculateOptimizedRoute(request);
        return ResponseEntity.ok(route);
    }

}
