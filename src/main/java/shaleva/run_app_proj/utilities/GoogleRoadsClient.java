package shaleva.run_app_proj.utilities;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import shaleva.run_app_proj.datamodels.RoadsResponse;

import java.util.Map; // זמני, עד שניצור את מחלקת התגובה של Roads

public interface GoogleRoadsClient {
    
    // פונקציה למגנוט נקודות לכביש/שביל הקרוב
    @GET("v1/snapToRoads")
    Call<RoadsResponse> snapToRoads(
        @Query("path") String path, // מחרוזת של קואורדינטות מופרדות ב- |
        @Query("interpolate") boolean interpolate
    );
}