package shaleva.run_app_proj.utilities;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import shaleva.run_app_proj.datamodels.DistanceMatrixResponse;

public interface GoogleDistanceMatrixClient {

    @GET("maps/api/distancematrix/json")
    Call<DistanceMatrixResponse> getDistances(
        @Query("origins") String origins,
        @Query("destinations") String destinations,
        @Query("mode") String mode // נשלח "walking"
    );
}