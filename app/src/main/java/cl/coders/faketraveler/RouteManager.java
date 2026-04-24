package cl.coders.faketraveler;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RouteManager {

    private static final String TAG = RouteManager.class.getSimpleName();
    private static final String OSRM_BASE_URL = "https://router.project-osrm.org/route/v1/";

    @NonNull
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    @NonNull
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface RouteCallback {
        void onSuccess(@NonNull ArrayList<double[]> points, double totalMeters);
        void onFailure(@NonNull String error);
    }

    public void fetchRoute(double latA, double lonA, double latB, double lonB, @NonNull String profile, @NonNull RouteCallback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            BufferedReader reader = null;
            try {
                String urlStr = OSRM_BASE_URL + profile + "/" + lonA + "," + latA + ";" + lonB + "," + latB + "?overview=full&geometries=geojson";
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", "FakeTraveler/1.0");

                int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    final String error = "HTTP error: " + responseCode;
                    Log.e(TAG, error);
                    postFailure(callback, error);
                    return;
                }

                StringBuilder response = new StringBuilder();
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                JSONObject json = new JSONObject(response.toString());
                JSONArray routes = json.optJSONArray("routes");
                if (routes == null || routes.length() == 0) {
                    postFailure(callback, "No route found");
                    return;
                }

                JSONObject firstRoute = routes.getJSONObject(0);
                JSONObject geometry = firstRoute.getJSONObject("geometry");
                JSONArray coords = geometry.getJSONArray("coordinates");

                ArrayList<double[]> points = new ArrayList<>();
                double totalDistance = 0;

                for (int i = 0; i < coords.length(); i++) {
                    JSONArray coord = coords.getJSONArray(i);
                    double lon = coord.getDouble(0);
                    double lat = coord.getDouble(1);
                    points.add(new double[]{lat, lon});

                    if (i > 0) {
                        double[] prev = points.get(i - 1);
                        totalDistance += GeoUtils.haversineMeters(prev[0], prev[1], lat, lon);
                    }
                }

                postSuccess(callback, points, totalDistance);

            } catch (Exception e) {
                Log.e(TAG, "Error fetching route", e);
                postFailure(callback, e.getMessage() != null ? e.getMessage() : "Unknown error");
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception ignored) {
                    }
                }
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    private void postSuccess(@NonNull RouteCallback callback, @NonNull ArrayList<double[]> points, double totalMeters) {
        mainHandler.post(() -> callback.onSuccess(points, totalMeters));
    }

    private void postFailure(@NonNull RouteCallback callback, @NonNull String error) {
        mainHandler.post(() -> callback.onFailure(error));
    }
}