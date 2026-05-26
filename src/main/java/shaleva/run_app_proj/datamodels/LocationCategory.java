package shaleva.run_app_proj.datamodels;

import java.util.ArrayList;
import java.util.List;

public enum LocationCategory {
    // קטגוריות לחיפוש (isSearchable = true)
    GREEN_ZONE(55.0, List.of("park", "garden", "playground"), List.of("nature_reserve"), true),
    SPORTS_AND_CAMPUS(25.0, List.of("stadium", "university", "sports_complex"), List.of(), true),
    SERVICE_POINTS(20.0, List.of("convenience_store", "gas_station"), List.of(), true),
    TRAFFIC_HUB(15.0, List.of("bus_station", "train_station", "transit_station"), List.of(), true),
    ATTRACTIONS(3.0, List.of("shopping_mall", "cafe", "bakery"), List.of(), true),

    // קטגוריות לניקוד בלבד (isSearchable = false)
    STREET(10.0, List.of(), List.of("route", "intersection"), false),
    DEFAULT(1.0, List.of(), List.of(), false);

    private final double baseScore;
    private final List<String> apiTypes;      // טיפוסים שמותר לשלוח ל-Places API
    private final List<String> extraTypes;    // טיפוסים שקיימים רק לניקוד (כמו רחוב או שמורת טבע)
    private final boolean isSearchable;

    LocationCategory(double baseScore, List<String> apiTypes, List<String> extraTypes, boolean isSearchable) {
        this.baseScore = baseScore;
        this.apiTypes = apiTypes;
        this.extraTypes = extraTypes;
        this.isSearchable = isSearchable;
    }

    public double getBaseScore() { return baseScore; }

    /**
     * מחזירה רשימה נקייה של כל הטיפוסים שמותר לשלוח בבקשת ה-Nearby Search
     */
    public static List<String> getGlobalSearchableTypes() {
        List<String> searchable = new ArrayList<>();
        for (LocationCategory category : values()) {
            if (category.isSearchable) {
                searchable.addAll(category.apiTypes);
            }
        }
        return searchable;
    }

    /**
     * סורקת את כל הטיפוסים (גם אלו של החיפוש וגם אלו של הניקוד בלבד)
     */
    public static double getHighestScore(List<String> googleTypes, List<String> bonusCategories) {
        if (googleTypes == null || googleTypes.isEmpty()) return DEFAULT.baseScore;
        
        double maxScore = DEFAULT.baseScore;
        for (String type : googleTypes) {
            for (LocationCategory category : values()) {
                // בודקים גם ב-apiTypes וגם ב-extraTypes
                if (category.apiTypes.contains(type) || category.extraTypes.contains(type)) {
                    if (bonusCategories.contains(category.name())) {maxScore = Math.max(maxScore, 1.2 * category.baseScore); System.out.println("BONUS!!!");}
                    else maxScore = Math.max(maxScore, category.baseScore);
                }
            }
        }
        return maxScore;
    }
}