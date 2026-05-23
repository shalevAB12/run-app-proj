package shaleva.run_app_proj.services;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import retrofit2.Response;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

import shaleva.run_app_proj.datamodels.Candidate;
import shaleva.run_app_proj.datamodels.DirectionsResponse;
import shaleva.run_app_proj.datamodels.DistanceMatrixResponse;
import shaleva.run_app_proj.datamodels.LocationCategory;
import shaleva.run_app_proj.datamodels.OptimizedRoute;
import shaleva.run_app_proj.datamodels.PlacesResponse;
import shaleva.run_app_proj.datamodels.RoadsResponse;
import shaleva.run_app_proj.datamodels.RouteRequestObject;
import shaleva.run_app_proj.datamodels.Waypoint;
import shaleva.run_app_proj.utilities.GoogleDirectionsClient;
import shaleva.run_app_proj.utilities.GoogleDistanceMatrixClient;
import shaleva.run_app_proj.utilities.GooglePlacesClient;
import shaleva.run_app_proj.utilities.GoogleRoadsClient;

@Service
public class RouteService {

    @Autowired private GooglePlacesClient placesClient;
    @Autowired private GoogleRoadsClient roadsClient;
    @Autowired private GoogleDistanceMatrixClient distanceMatrixClient;
    @Autowired private GoogleDirectionsClient directionsClient;
    @Autowired private DPSOAlgorithmService dpsoAlgorithmService;

    // קבועים - נשארים ברמת ה-Service
    private static final int PER_KILOMETER_FACTOR = 15;
    private static final double CELL_AMOUNT_FACTOR = 1;
    private static final double K_FACTOR = 1.8;
    private static final int K_LIMIT = 23;
    private static final int ITERATIONS_BASE = 50;
    private static final int SWARM_SIZE_BASE = 75;
    private static final String FIELD_MASK = "places.id,places.location,places.displayName,places.rating,places.userRatingCount,places.types";


    private static class RouteContext {
        double maxLength;
        double pointsSearchRadius;
        int pointsAmount;
        int cellsAmount;
        int cellsPerAxis;
        double cellSize;
        double minPtoPDistance;
        int k;
        int iterations;
        int swarmSize;

        // Counters
        int countPark = 0;
        int countRoad = 0;
        int countRaw = 0;

        public RouteContext(double maxDist) {
            this.maxLength = maxDist;
            this.pointsSearchRadius = maxDist * 0.5;
            this.pointsAmount = (int) (maxDist * PER_KILOMETER_FACTOR / 1000.0);
            this.cellsAmount = (int) Math.pow(Math.ceil(Math.sqrt(pointsAmount * CELL_AMOUNT_FACTOR)), 2);
            this.cellsPerAxis = (int) Math.sqrt(cellsAmount);
            this.cellSize = (2 * pointsSearchRadius) / cellsPerAxis;
            this.minPtoPDistance = cellSize / 2;
            this.k = (int) (Math.min(Math.sqrt(pointsAmount) * K_FACTOR, K_LIMIT));
            this.iterations = ITERATIONS_BASE + pointsAmount * 5;
            this.swarmSize = (int) (SWARM_SIZE_BASE + pointsAmount * 1.5);
        }
    }

    public OptimizedRoute calculateOptimizedRoute(RouteRequestObject request) {
        System.out.println("LAT: " + request.getStartLat());
        System.out.println("LNG: " + request.getStartLng());
        System.out.println("DIST: " + request.getDistance());

        // יצירת קונטקסט לריצה הנוכחית
        RouteContext ctx = new RouteContext(request.getDistance());
        
        List<Candidate> list = buildGridAndFetchPoints(ctx, request.getStartLat(), request.getStartLng());
        list = filterRawCenterCandidates(list);
        list = pruneToTargetSize(list, ctx.pointsAmount, request.getStartLat(), request.getStartLng());

        Candidate startNode = new Candidate("START", request.getStartLat(), request.getStartLng(), 0);
        list.add(0, startNode);

        Map<Candidate, List<Candidate>> knnGraph = getKNearestNeighbors(ctx, list, request.isCircular(), startNode);
        Map<Candidate, Map<Candidate, Double>> distanceMap = fetchRealWalkingDistances(knnGraph);

        List<Waypoint> solution = dpsoAlgorithmService.getSolution(ctx.maxLength, ctx.iterations, ctx.swarmSize,
                list, distanceMap, request.isCircular(), startNode);

        printRoute(solution);
        System.out.println("\n\n\n\n");
        printAllAsGeoJSON(list);

        return new OptimizedRoute(solution, fetchOverviewPolyline(solution));
    }

    private List<Candidate> buildGridAndFetchPoints(RouteContext ctx, double userLat, double userLng) {
        List<Candidate> candidates = new ArrayList<>();

        double latStep = ctx.cellSize / 111320.0;
        double lngStep = ctx.cellSize / (111320.0 * Math.cos(Math.toRadians(userLat)));

        double startLat = userLat - (ctx.pointsSearchRadius / 111320.0);
        double startLng = userLng - (ctx.pointsSearchRadius / (111320.0 * Math.cos(Math.toRadians(userLat))));

        for (int i = 0; i < ctx.cellsPerAxis; i++) {
            for (int j = 0; j < ctx.cellsPerAxis; j++) {
                double cellLat = startLat + (i * latStep) + (latStep / 2);
                double cellLng = startLng + (j * lngStep) + (lngStep / 2);

                if (isWithinRadius(userLat, userLng, cellLat, cellLng, ctx.pointsSearchRadius)) {
                    Candidate bestInCell = fetchBestInCell(cellLat, cellLng, ctx.cellSize / 2);

                    if (bestInCell != null) {
                        candidates.add(bestInCell);
                        ctx.countPark++;
                    } else {
                        Candidate snappedCandidate = fetchSnappedPoint(cellLat, cellLng, i, j);
                        if (snappedCandidate != null) {
                            candidates.add(snappedCandidate);
                            if (snappedCandidate.getId().contains("raw_center")) {
                                ctx.countRaw++;
                            } else {
                                ctx.countRoad++;
                            }
                        }
                    }
                }
            }
        }
        return candidates;
    }

    private Candidate fetchBestInCell(double lat, double lng, double rad) {
        try {
            Map<String, Object> body = Map.of(
                    "maxResultCount", 5,
                    "includedTypes", LocationCategory.getGlobalSearchableTypes(),
                    "locationRestriction", Map.of("circle", Map.of(
                            "center", Map.of("latitude", lat, "longitude", lng),
                            "radius", rad)));

            Response<PlacesResponse> response = placesClient.searchNearby(FIELD_MASK, body).execute();

            if (response.isSuccessful() && response.body() != null &&
                    response.body().getPlaces() != null && !response.body().getPlaces().isEmpty()) {
                return getMaxRewardPlaceCandidate(response.body().getPlaces());
            } else if (!response.isSuccessful()) {
                System.out.println("Places API Error: " + response.code());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Candidate fetchSnappedPoint(double lat, double lng, int gridI, int gridJ) {
        try {
            String path = lat + "," + lng;
            Response<RoadsResponse> response = roadsClient.snapToRoads(path, false).execute();

            if (response.isSuccessful() && response.body() != null &&
                    response.body().getSnappedPoints() != null && !response.body().getSnappedPoints().isEmpty()) {
                RoadsResponse.SnappedPoint snapped = response.body().getSnappedPoints().get(0);
                String id = (snapped.getPlaceId() != null) ? snapped.getPlaceId()
                        : "generic_node_" + gridI + "_" + gridJ;
                double streetScore = LocationCategory.STREET.getBaseScore();
                return new Candidate(id, snapped.getLocation().getLatitude(),
                        snapped.getLocation().getLongitude(), streetScore);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Candidate("raw_center_" + gridI + "_" + gridJ, lat, lng, 0.0);
    }

    private boolean isWithinRadius(double lat1, double lng1, double lat2, double lng2, double radius) {
        double dLat = (lat2 - lat1) * 111320.0;
        double dLng = (lng2 - lng1) * 111320.0 * Math.cos(Math.toRadians(lat1));
        return Math.sqrt(dLat * dLat + dLng * dLng) <= radius;
    }

    private List<Candidate> filterRawCenterCandidates(List<Candidate> list) {
        List<Candidate> filtered = new ArrayList<>();
        for (Candidate candidate : list) {
            if (!candidate.getId().contains("raw_center")) {
                filtered.add(candidate);
            }
        }
        return filtered;
    }

    private List<Candidate> pruneToTargetSize(List<Candidate> candidates, int targetSize, double centerLat, double centerLng) {
        if (candidates.size() <= targetSize) return candidates;

        candidates.sort((c1, c2) -> {
            int rewardCompare = Double.compare(c2.getReward(), c1.getReward());
            if (rewardCompare != 0) return rewardCompare;
            double dist1 = calculateEuclideanToCenter(c1, centerLat, centerLng);
            double dist2 = calculateEuclideanToCenter(c2, centerLat, centerLng);
            return Double.compare(dist1, dist2);
        });

        return new ArrayList<>(candidates.subList(0, targetSize));
    }

    private double calculateCandidateReward(double baseScore, Double rating, Integer reviews) {
        double reward = baseScore;
        double r = (rating != null) ? rating : 0.0;
        int rev = (reviews != null) ? reviews : 0;
        if (r > 0 && rev > 0) {
            reward += (r * 2.5 * Math.log(rev + 1));
        }
        return reward;
    }

    private Candidate getMaxRewardPlaceCandidate(List<PlacesResponse.Place> places) {
        PlacesResponse.Place bestPlace = null;
        double maxReward = -1.0;

        for (PlacesResponse.Place place : places) {
            double baseScore = LocationCategory.getHighestScore(place.getTypes());
            double currentReward = calculateCandidateReward(baseScore, place.getRating(), place.getUserRatingCount());

            if (currentReward > maxReward) {
                maxReward = currentReward;
                bestPlace = place;
            }
        }

        if (bestPlace != null) {
            return new Candidate(bestPlace.getId(), bestPlace.getLocation().getLatitude(),
                    bestPlace.getLocation().getLongitude(), maxReward);
        }
        return null;
    }

    private Map<Candidate, List<Candidate>> getKNearestNeighbors(RouteContext ctx, List<Candidate> candidates, boolean isCircular, Candidate startNode) {
        Map<Candidate, List<Candidate>> graph = new HashMap<>();

        for (Candidate current : candidates) {
            List<Candidate> others = new ArrayList<>(candidates);
            others.remove(current);

            others.sort((c1, c2) -> {
                double dist1 = calculateEuclideanDist(current, c1);
                double dist2 = calculateEuclideanDist(current, c2);
                return Double.compare(dist1, dist2);
            });

            int limit = Math.min(ctx.k, others.size());
            List<Candidate> nearestNeighbors = new ArrayList<>(others.subList(0, limit));

            if (isCircular && !current.equals(candidates.get(0))) {
            if (!nearestNeighbors.contains(candidates.get(0))) {
                nearestNeighbors.add(candidates.get(0));
            }
        }
            graph.put(current, nearestNeighbors);
        }
        return graph;
    }

    private double calculateEuclideanDist(Candidate a, Candidate b) {
        double dLat = (a.getLat() - b.getLat()) * 111320.0;
        double dLng = (a.getLng() - b.getLng()) * 111320.0 * Math.cos(Math.toRadians(a.getLat()));
        return Math.sqrt(dLat * dLat + dLng * dLng);
    }

    private double calculateEuclideanToCenter(Candidate c, double centerLat, double centerLng) {
        double dLat = (c.getLat() - centerLat) * 111320.0;
        double dLng = (c.getLng() - centerLng) * 111320.0 * Math.cos(Math.toRadians(c.getLat()));
        return Math.sqrt(dLat * dLat + dLng * dLng);
    }

    private Map<Candidate, Map<Candidate, Double>> fetchRealWalkingDistances(Map<Candidate, List<Candidate>> knnGraph) {
        Map<Candidate, Map<Candidate, Double>> distanceMap = new HashMap<>();

        for (Map.Entry<Candidate, List<Candidate>> entry : knnGraph.entrySet()) {
            Candidate origin = entry.getKey();
            List<Candidate> destinations = entry.getValue();
            distanceMap.put(origin, new HashMap<>());

            if (destinations.isEmpty()) continue;

            StringBuilder destParam = buildAPIRequestDestPointsFormat(destinations);

            try {
                Response<DistanceMatrixResponse> response = distanceMatrixClient.getDistances(
                        formatLocation(origin), destParam.toString(), "walking").execute();

                if (response.isSuccessful() && response.body() != null && "OK".equals(response.body().getStatus())) {
                    DistanceMatrixResponse.Row row = response.body().getRows().get(0);
                    updateDistanceResultsToMap(origin, destinations, row, distanceMap);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return distanceMap;
    }

    private String formatLocation(Candidate c) {
        return c.getLat() + "," + c.getLng();
    }

    private StringBuilder buildAPIRequestDestPointsFormat(List<Candidate> destinations) {
        StringBuilder destParam = new StringBuilder();
        for (int i = 0; i < destinations.size(); i++) {
            destParam.append(formatLocation(destinations.get(i)));
            if (i < destinations.size() - 1) {
                destParam.append("|");
            }
        }
        return destParam;
    }

    private void updateDistanceResultsToMap(Candidate origin, List<Candidate> destinations,
            DistanceMatrixResponse.Row row, Map<Candidate, Map<Candidate, Double>> distanceMap) {
        for (int i = 0; i < destinations.size(); i++) {
            DistanceMatrixResponse.Element element = row.getElements().get(i);
            Candidate destination = destinations.get(i);
            distanceMap.putIfAbsent(destination, new HashMap<>());

            if ("OK".equals(element.getStatus())) {
                double distInMeters = element.getDistance().getValue();
                distanceMap.get(origin).put(destination, distInMeters);
                distanceMap.get(destination).putIfAbsent(origin, distInMeters);
            } else {
                distanceMap.get(origin).put(destination, Double.POSITIVE_INFINITY);
                distanceMap.get(destination).putIfAbsent(origin, Double.POSITIVE_INFINITY);
            }
        }
    }

    private String fetchOverviewPolyline(List<Waypoint> waypoints) {
        if (waypoints == null || waypoints.size() < 2) return "";

        Waypoint originWp = waypoints.get(0);
        String originParam = originWp.getLat() + "," + originWp.getLng();
        Waypoint destWp = waypoints.get(waypoints.size() - 1);
        String destParam = destWp.getLat() + "," + destWp.getLng();
        String waypointsParam = formatIntermediateWaypoints(waypoints);

        try {
            Response<DirectionsResponse> response = directionsClient
                    .getDirections(originParam, destParam, waypointsParam, "walking").execute();

            if (response.isSuccessful() && response.body() != null && "OK".equals(response.body().getStatus())) {
                List<DirectionsResponse.Route> routes = response.body().getRoutes();
                if (routes != null && !routes.isEmpty()) {
                    return routes.get(0).getOverview_polyline().getPoints();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String formatIntermediateWaypoints(List<Waypoint> waypoints) {
        if (waypoints.size() <= 2) return null;
        StringBuilder sb = new StringBuilder("optimize:false");
        for (int i = 1; i < waypoints.size() - 1; i++) {
            Waypoint wp = waypoints.get(i);
            sb.append("|").append(wp.getLat()).append(",").append(wp.getLng());
        }
        return sb.toString();
    }

    public void printAllAsGeoJSON(List<Candidate> candidates) {
        System.out.println("--- Copy from here to geojson.io ---");
        System.out.println("{ \"type\": \"FeatureCollection\", \"features\": [");
        for (int i = 0; i < candidates.size(); i++) {
            Candidate c = candidates.get(i);
            System.out.print(String.format(
                    "{ \"type\": \"Feature\", \"properties\": { \"id\": \"%s\", \"reward\": %f }, \"geometry\": { \"type\": \"Point\", \"coordinates\": [ %f, %f ] } }",
                    c.getId(), c.getReward(), c.getLng(), c.getLat()));
            if (i < candidates.size() - 1) System.out.println(",");
        }
        System.out.println("\n] }");
    }

    public void printRoute(List<Waypoint> result) {
        for (int i = 0; i < result.size(); i++) {
            System.out.println("WAYPOINT --- ID -> " + result.get(i).getPlaceId());
            if (i != result.size() - 1)
                System.out.println("DISTANCE: " + result.get(i).getDistanceToNext());
        }
    }
}