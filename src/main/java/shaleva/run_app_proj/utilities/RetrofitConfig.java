package shaleva.run_app_proj.utilities;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Configuration
public class RetrofitConfig {

    @Value("${google.api.key}")
    private String apiKey;

    // 1. הלקוח המרכזי - עושה את הלוגים ומזריק את המפתח לכולם
    @Bean
    public OkHttpClient googleOkHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        return new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(chain -> {
                    Request original = chain.request();

                    // הוספת המפתח כ-Query Parameter (חובה עבור Roads API ושירותים ישנים)
                    HttpUrl urlWithQuery = original.url().newBuilder()
                            .addQueryParameter("key", apiKey)
                            .build();

                    // הוספת המפתח כ-Header (חובה עבור Places API New)
                    Request requestBuilder = original.newBuilder()
                            .url(urlWithQuery)
                            .addHeader("X-Goog-Api-Key", apiKey)
                            .build();

                    return chain.proceed(requestBuilder);
                })
                .build();
    }

    // 2. הלקוח של Places API
    @Bean
    public GooglePlacesClient googlePlacesClient(OkHttpClient googleOkHttpClient) {
        return new Retrofit.Builder()
                .baseUrl("https://places.googleapis.com/")
                .addConverterFactory(JacksonConverterFactory.create())
                .client(googleOkHttpClient) // משתמשים בלקוח המשותף
                .build()
                .create(GooglePlacesClient.class);
    }

    // 3. הלקוח החדש של Roads API
    @Bean
    public GoogleRoadsClient googleRoadsClient(OkHttpClient googleOkHttpClient) {
        return new Retrofit.Builder()
                .baseUrl("https://roads.googleapis.com/")
                .addConverterFactory(JacksonConverterFactory.create())
                .client(googleOkHttpClient) // משתמשים באותו לקוח משותף!
                .build()
                .create(GoogleRoadsClient.class);
    }

    // 4. הלקוח של Distance Matrix API
    @Bean
    public GoogleDistanceMatrixClient googleDistanceMatrixClient(OkHttpClient googleOkHttpClient) {
        return new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/") // כתובת ה-Base עבור Maps API
                .addConverterFactory(JacksonConverterFactory.create())
                .client(googleOkHttpClient)
                .build()
                .create(GoogleDistanceMatrixClient.class);
    }

// 5. הלקוח של Directions API
    @Bean
    public GoogleDirectionsClient googleDirectionsClient(OkHttpClient googleOkHttpClient) {
        return new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/") // משתמש באותו ה-Base URL של ה-Distance Matrix
                .addConverterFactory(JacksonConverterFactory.create())
                .client(googleOkHttpClient) // הזרקת הלקוח המשותף שמטפל בלוגים ובמפתחות
                .build()
                .create(GoogleDirectionsClient.class);
    }
}