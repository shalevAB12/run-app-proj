package shaleva.run_app_proj.utilities;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import shaleva.run_app_proj.datamodels.PlacesResponse;

import java.util.Map;

// כותרת שצריכה לבכיל את התגובה שהתקבלה מה-places api כאובייקט PlacesResponse
public interface GooglePlacesClient {
    @POST("v1/places:searchNearby")
    Call<PlacesResponse> searchNearby(
        @Header("X-Goog-FieldMask") String fieldMask,
        @Body Map<String, Object> body
    );
}
