package shaleva.run_app_proj.utilities;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import shaleva.run_app_proj.datamodels.DirectionsResponse;

public interface GoogleDirectionsClient {
    @GET("maps/api/directions/json")
    Call<DirectionsResponse> getDirections(
            @Query("origin") String origin,
            @Query("destination") String destination,
            @Query("waypoints") String waypoints,
            @Query("mode") String mode //walking
    );
}