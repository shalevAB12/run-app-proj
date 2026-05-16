package shaleva.run_app_proj.services;

// Spring Framework Annotations
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

// HTTP Client (Retrofit)
import retrofit2.Response;

// Java Utilities
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.IOException;

// DTOs
import shaleva.run_app_proj.datamodels.Candidate;
import shaleva.run_app_proj.datamodels.DistanceMatrixResponse;
import shaleva.run_app_proj.datamodels.DistanceMatrixResponse.Row;
import shaleva.run_app_proj.datamodels.LocationCategory;
import shaleva.run_app_proj.datamodels.PlacesResponse;
import shaleva.run_app_proj.datamodels.RoadsResponse; // הייבוא החדש
import shaleva.run_app_proj.datamodels.RouteRequest;
import shaleva.run_app_proj.datamodels.Waypoint;
import shaleva.run_app_proj.utilities.GoogleDistanceMatrixClient;
// Clients
import shaleva.run_app_proj.utilities.GooglePlacesClient;
import shaleva.run_app_proj.utilities.GoogleRoadsClient; // הייבוא החדש

@Service
public class RouteService {

    @Autowired
    private GooglePlacesClient placesClient;

    @Autowired
    private GoogleRoadsClient roadsClient;

    @Autowired
    private GoogleDistanceMatrixClient distanceMatrixClient;

    @Autowired
    private DPSOAlgorithmService dpsoAlgorithmService;

    private static final int PER_KILOMETER_FACTOR = 15; // 20
    private static final double CELL_AMOUNT_FACTOR = 1; // 2
    private static final double K_FACTOR = 1.8;
    private static final int K_LIMIT = 23;
    private static final int ITERATIONS_BASE = 50;
    private static final int SWARM_SIZE_BASE = 75;
    private static final String FIELD_MASK = "places.id,places.location,places.displayName,places.rating,places.userRatingCount,places.types";

    private double maxLength;
    private double pointsSearchRadius;
    private int pointsAmount;
    private int cellsAmount;
    private int cellsPerAxis;
    private double cellSize;
    private double minPtoPDistance;
    private int k;

    private int iterations;
    private int swarmSize;

    private int countPark = 0;
    private int countRoad = 0;
    private int countRaw = 0;

    public List<Waypoint> calculateOptimizedRoute(RouteRequest request) {

        List<Candidate> list = buildGridAndFetchPoints(request.getStartLat(), request.getStartLng(),
                request.getDistance());
        list = filterRawCenterCandidates(list);
        list = pruneToTargetSize(list, pointsAmount, request.getStartLat(), request.getStartLng());
        list.add(0, new Candidate("START", request.getStartLat(), request.getStartLng(), 0));

        Map<Candidate, List<Candidate>> knnGraph = getKNearestNeighbors(list);
        Map<Candidate, Map<Candidate, Double>> distanceMap = fetchRealWalkingDistances(knnGraph);

        List<Waypoint> solution = dpsoAlgorithmService.getSolution(maxLength, iterations, swarmSize,
                list, distanceMap);

        printRoute(solution);
        System.out.println("\n\n\n\n");
        printAllAsGeoJSON(list);

        return solution;

    }

    public List<Candidate> buildGridAndFetchPoints(double userLat, double userLng, double maxDist) {
        List<Candidate> candidates = new ArrayList<>();

        initPointsFetchingVariables(maxDist);

        double latStep = cellSize / 111320.0;
        double lngStep = cellSize / (111320.0 * Math.cos(Math.toRadians(userLat)));

        double startLat = userLat - (pointsSearchRadius / 111320.0);
        double startLng = userLng - (pointsSearchRadius / (111320.0 * Math.cos(Math.toRadians(userLat))));

        for (int i = 0; i < cellsPerAxis; i++) {
            for (int j = 0; j < cellsPerAxis; j++) {

                double cellLat = startLat + (i * latStep) + (latStep / 2);
                double cellLng = startLng + (j * lngStep) + (lngStep / 2);

                if (isWithinRadius(userLat, userLng, cellLat, cellLng, pointsSearchRadius)) {

                    Candidate bestInCell = fetchBestInCell(cellLat, cellLng, cellSize / 2);

                    if (bestInCell != null) {
                        candidates.add(bestInCell);
                        countPark++;
                    } else {
                        // הוספת נקודת צומת גנרית עם Roads API
                        Candidate snappedCandidate = fetchSnappedPoint(cellLat, cellLng, i, j);
                        if (snappedCandidate != null) {
                            candidates.add(snappedCandidate);
                            if (snappedCandidate.getId().contains("raw_center")) {
                                countRaw++;
                            } else
                                countRoad++;
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
                System.out.println("Places API Error: " + response.code() + " " + response.errorBody().string());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Candidate fetchSnappedPoint(double lat, double lng, int gridI, int gridJ) {
        try {
            // הכנת המחרוזת לפי דרישות ה-Roads API (Lat,Lng)
            String path = lat + "," + lng;

            // שליחה ל-API. interpolate=false כי אנחנו רק רוצים למגנט את הנקודה הבודדת
            Response<RoadsResponse> response = roadsClient.snapToRoads(path, false).execute();

            if (response.isSuccessful() && response.body() != null &&
                    response.body().getSnappedPoints() != null && !response.body().getSnappedPoints().isEmpty()) {

                RoadsResponse.SnappedPoint snapped = response.body().getSnappedPoints().get(0);

                // שימוש ב-PlaceId של הכביש אם יש, אחרת יצירת מזהה גנרי שמציין שזה צומת
                String id = (snapped.getPlaceId() != null) ? snapped.getPlaceId()
                        : "generic_node_" + gridI + "_" + gridJ;

                // שים לב: Reward של 0 כדי לא לפגוע באיכות המסלול
                double streetScore = LocationCategory.STREET.getBaseScore();
                return new Candidate(id, snapped.getLocation().getLatitude(),
                        snapped.getLocation().getLongitude(), streetScore);

            } else if (!response.isSuccessful()) {
                System.out.println("Roads API Error: " + response.code() + " " + response.errorBody().string());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Fallback: במקרה שהקריאה ל-Roads נכשלה, עדיין נחזיר את מרכז התא כדי לא לשבור
        // את הרשת
        return new Candidate("raw_center_" + gridI + "_" + gridJ, lat, lng, 0.0);
    }

    private void initPointsFetchingVariables(double maxLength) {
        this.maxLength = maxLength;
        this.pointsSearchRadius = maxLength * 0.5;
        this.pointsAmount = (int) (maxLength * PER_KILOMETER_FACTOR / 1000.0);
        this.cellsAmount = (int) Math.pow(Math.ceil(Math.sqrt(pointsAmount * CELL_AMOUNT_FACTOR)), 2);
        this.cellsPerAxis = (int) Math.sqrt(cellsAmount);
        this.cellSize = (2 * pointsSearchRadius) / cellsPerAxis;
        this.minPtoPDistance = cellSize / 2;
        this.k = (int) (Math.min(Math.sqrt(pointsAmount) * K_FACTOR, K_LIMIT));

        this.iterations = ITERATIONS_BASE + pointsAmount * 5;
        this.swarmSize = (int) (SWARM_SIZE_BASE + pointsAmount * 1.5);
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

    private List<Candidate> pruneToTargetSize(List<Candidate> candidates, int targetSize, double centerLat,
            double centerLng) {
        if (candidates.size() <= targetSize)
            return candidates;

        // מיון הרשימה לפי סדר חשיבות:
        candidates.sort((c1, c2) -> {
            // 1. עדיפות ראשונה: סוג המקום (פארקים תמיד קודמים)
            // (בהנחה שה-Reward כבר משקלל את ה-BaseScore של ה-Enum)
            int rewardCompare = Double.compare(c2.getReward(), c1.getReward());
            if (rewardCompare != 0)
                return rewardCompare;

            // 2. עדיפות שניה (שובר שוויון): קרבה למרכז הגריד
            double dist1 = calculateEuclideanToCenter(c1, centerLat, centerLng);
            double dist2 = calculateEuclideanToCenter(c2, centerLat, centerLng);
            return Double.compare(dist1, dist2);
        });

        // מחזירים רק את ה-targetSize הנקודות הטובות ביותר
        return new ArrayList<>(candidates.subList(0, targetSize));
    }



    private double calculateCandidateReward(double baseScore, Double rating, Integer reviews) {
        double reward = baseScore;
        double r = (rating != null) ? rating : 0.0;
        int rev = (reviews != null) ? reviews : 0;

        if (r > 0 && rev > 0) {
            // שימוש ב-Math.log עבור ln
            reward += (r * 2.5 * Math.log(rev + 1));
        }
        return reward;
    }

    private Candidate getMaxRewardPlaceCandidate(List<PlacesResponse.Place> places) {
        PlacesResponse.Place bestPlace = null;
        double maxReward = -1.0;

        for (PlacesResponse.Place place : places) {
            // 1. זיהוי קטגוריה וניקוד בסיס
            double baseScore = LocationCategory.getHighestScore(place.getTypes());

            // 2. חישוב Reward סופי (כולל בונוס דירוג ו-ln)
            double currentReward = calculateCandidateReward(
                    baseScore,
                    place.getRating(),
                    place.getUserRatingCount());

            if (currentReward > maxReward) {
                maxReward = currentReward;
                bestPlace = place;
            }
        }

        if (bestPlace != null) {
            return new Candidate(
                    bestPlace.getId(),
                    bestPlace.getLocation().getLatitude(),
                    bestPlace.getLocation().getLongitude(),
                    maxReward // ה-Reward המחושב נשמר כאן
            );
        }

        return null;
    }



    private Map<Candidate, List<Candidate>> getKNearestNeighbors(List<Candidate> candidates) {
        Map<Candidate, List<Candidate>> graph = new HashMap<>();

        for (Candidate current : candidates) {
            // 1. יצירת רשימה של כל הנקודות מלבד הנקודה הנוכחית
            List<Candidate> others = new ArrayList<>(candidates);
            others.remove(current);

            // 2. מיון הרשימה לפי המרחק לנקודה הנוכחית (מהקרוב לרחוק)
            others.sort((c1, c2) -> {
                double dist1 = calculateEuclideanDist(current, c1);
                double dist2 = calculateEuclideanDist(current, c2);
                return Double.compare(dist1, dist2);
            });

            // 3. חיתוך הרשימה - לוקחים רק את K הראשונים
            // (משתמשים ב-Math.min למקרה שיש פחות מ-K מועמדים ברשימה כולה)
            int limit = Math.min(k, others.size());

            // עוטפים ב-ArrayList חדש כדי שהרשימה תהיה ניתנת לשינוי בעתיד אם נצטרך
            List<Candidate> nearestNeighbors = new ArrayList<>(others.subList(0, limit));

            // 4. שמירת הנקודה והשכנים שלה במפה
            graph.put(current, nearestNeighbors);
        }

        return graph;
    }

    private double calculateEuclideanDist(Candidate a, Candidate b) {
        // 111,320 מטרים שווים בערך למעלה אחת
        double dLat = (a.getLat() - b.getLat()) * 111320.0;
        // בקפיצות של קווי אורך צריך להתחשב בקו הרוחב 
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

            // אתחול המפה הפנימית עבור נקודת המוצא הזו
            distanceMap.put(origin, new HashMap<>());

            if (destinations.isEmpty())
                continue;

            // בניית מחרוזת היעדים (גוגל דורשת הפרדה באמצעות התו '|')
            StringBuilder destParam = buildAPIRequestDestPointsFormat(destinations);

            try {
                // קריאה אחת ל-API עבור נקודת מוצא מול כל השכנים
                Response<DistanceMatrixResponse> response = sendAndGetDMResponse(origin, destParam);

                if (response.isSuccessful() && response.body() != null && "OK".equals(response.body().getStatus())) {
                    DistanceMatrixResponse.Row row = response.body().getRows().get(0); // יש לנו רק origin אחד בבקשה
                    // עוברים על התוצאות וממפים אותן חזרה ל-Candidates
                    updateDistanceResultsToMap(origin, destinations, row, distanceMap);

                } else if (response.body() == null) {
                    System.out.println("===========NULL===========MATX");
                } else {
                    System.out.println("Distance Matrix API Error for origin: " + origin.getId());
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
            DistanceMatrixResponse.Row row,
            Map<Candidate, Map<Candidate, Double>> distanceMap) {

        for (int i = 0; i < destinations.size(); i++) {
            DistanceMatrixResponse.Element element = row.getElements().get(i);
            Candidate destination = destinations.get(i);

            // שלב מקדים: מוודאים שהמפה הפנימית קיימת עבור היעד (כדי שנוכל להזריק לה את
            // הכיוון ההפוך)
            distanceMap.putIfAbsent(destination, new HashMap<>());

            if ("OK".equals(element.getStatus())) {
                double distInMeters = element.getDistance().getValue();

                //  שמירת הכיוון המקורי (A -> B) שגוגל הרגע חזר איתו
                distanceMap.get(origin).put(destination, distInMeters);

                distanceMap.get(destination).putIfAbsent(origin, distInMeters);

            } else {
                // אם אין מסלול הליכה - שמים אינסוף בשני הכיוונים
                distanceMap.get(origin).put(destination, Double.POSITIVE_INFINITY);
                distanceMap.get(destination).putIfAbsent(origin, Double.POSITIVE_INFINITY);
            }
        }
    }

    private Response<DistanceMatrixResponse> sendAndGetDMResponse(Candidate origin, StringBuilder destParam)
            throws IOException {
        return distanceMatrixClient.getDistances(
                formatLocation(origin),
                destParam.toString(),
                "walking" // חשוב מאוד - אנחנו מנווטים הולכי רגל/רצים!
        ).execute();
    }





    public void printCandidates(RouteRequest request) {
        System.out.println("--- מתחיל חישוב עבור מרחק: " + request.getDistance() + " ---");
        List<Candidate> list = buildGridAndFetchPoints(request.getStartLat(), request.getStartLng(),
                request.getDistance());
        list = filterRawCenterCandidates(list);

        // if (list == null || list.isEmpty()) {
        // System.out.println("⚠️ אזהרה: הרשימה חזרה ריקה מה-buildGridAndFetchPoints!");
        // } else {
        // System.out.println("✅ נמצאו " + list.size() + " מועמדים (פארקים וצמתים
        // ממוגנטים).");
        // }

        // for (Candidate candidate : list) {
        // System.out.println(String.format("{ CANDIDATE }:\nId ---> %s\nLat --->
        // %f\nLng ---> %f\nReward ---> %f\n",
        // candidate.getId(), candidate.getLat(), candidate.getLng(),
        // candidate.getReward()));
        // }

        System.out.println("/////////////////----START----//////////////\n\n\n\n");
        printAllAsGeoJSON(list);
        System.out.println("222222222//////////////////////////////////\n\n\n\n\n");
        printRoadAsGeoJSON(list);
        System.out.println("333333333//////////////////////////////////\n\n\n\n\n");

        System.out.println(maxLength);
        System.out.println(pointsSearchRadius);
        System.out.println(pointsAmount);
        System.out.println();
        System.out.println(cellsAmount);
        System.out.println(cellSize);

        System.out.println("places ------> " + countPark);
        System.out.println("roads ------> " + countRoad);
        System.out.println("raw ------> " + countRaw);
        System.out.println("raw-filtered -------> " + (countPark + countRoad));

        list = pruneToTargetSize(list, pointsAmount, request.getStartLat(), request.getStartLng());

        System.out.println("pruned ----------> " + list.size() + "\n\n\n\n");
        printAllAsGeoJSON(list);
        System.out.println("444444444//////////////////////////////////\n\n\n\n\n");
        printRoadAsGeoJSON(list);
        System.out.println("555555555//////////////////////////////////\n\n\n\n\n");

    }

    public void printAllAsGeoJSON(List<Candidate> candidates) {
        System.out.println("--- Copy from here to geojson.io ---");
        System.out.println("{ \"type\": \"FeatureCollection\", \"features\": [");
        for (int i = 0; i < candidates.size(); i++) {
            Candidate c = candidates.get(i);
            System.out.print(String.format(
                    "{ \"type\": \"Feature\", \"properties\": { \"id\": \"%s\", \"reward\": %f }, \"geometry\": { \"type\": \"Point\", \"coordinates\": [ %f, %f ] } }",
                    c.getId(), c.getReward(), c.getLng(), c.getLat()));
            if (i < candidates.size() - 1)
                System.out.println(",");
        }
        System.out.println("\n] }");
    }

    public void printRoadAsGeoJSON(List<Candidate> candidates) {
        System.out.println("--- Copy from here to geojson.io ---");
        System.out.println("{ \"type\": \"FeatureCollection\", \"features\": [");
        for (int i = 0; i < candidates.size(); i++) {
            Candidate c = candidates.get(i);

            if (!c.getId().contains("raw_center")) {
                System.out.print(String.format(
                        "{ \"type\": \"Feature\", \"properties\": { \"id\": \"%s\", \"reward\": %f }, \"geometry\": { \"type\": \"Point\", \"coordinates\": [ %f, %f ] } }",
                        c.getId(), c.getReward(), c.getLng(), c.getLat()));
                if (i < candidates.size() - 1)
                    System.out.println(",");
            }

        }
        System.out.println("\n] }");
    }

    public void printRawAsGeoJSON(List<Candidate> candidates) {
        System.out.println("--- Copy from here to geojson.io ---");
        System.out.println("{ \"type\": \"FeatureCollection\", \"features\": [");
        for (int i = 0; i < candidates.size(); i++) {
            Candidate c = candidates.get(i);

            if (c.getId().contains("raw_center")) {
                System.out.print(String.format(
                        "{ \"type\": \"Feature\", \"properties\": { \"id\": \"%s\", \"reward\": %f }, \"geometry\": { \"type\": \"Point\", \"coordinates\": [ %f, %f ] } }",
                        c.getId(), c.getReward(), c.getLng(), c.getLat()));
                if (i < candidates.size() - 1)
                    System.out.println(",");
            }
        }
        System.out.println("\n] }");
    }

    public void printRoute(List<Waypoint> result) {
        for (int i = 0; i < result.size(); i++) {
            System.out.println("WAYPOINT --- ID -> " + result.get(i).getPlaceId());
            System.out.println("lat --> " + result.get(i).getLat());
            System.out.println("lng --> " + result.get(i).getLng());
            System.out.println("reward --> " + result.get(i).getReward());
            if (i != result.size() - 1)
                System.out.println(" \n | |\n | | " + "DISTANCE: " + result.get(i).getDistanceToNext()
                        + " + \n __\n ****\n  **\n   *");
        }
    }

}