package com.example.moti;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class map_activity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private Button btnGetCurrentLocation, btnCalculateRoute;
    private EditText etDestination;
    private ProgressBar progressBar;

    private GeoPoint currentLocation;
    private GeoPoint destinationLocation;
    private RouteResult lastRouteResult;
    private String lastDestination;

    // Permission
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    // GraphHopper API Configuration
    private static final String GRAPHHOPPER_BASE_URL = "https://graphhopper.com/api/1/route";
    private static final String GRAPHHOPPER_API_KEY = "634bfc8e-2de1-4e92-80de-83f7f64836ac";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        initializeViews();
        setupMap();
        setupBottomNavigation();
    }

    private void initializeViews() {
        mapView = findViewById(R.id.mapView);
        btnGetCurrentLocation = findViewById(R.id.btnGetCurrentLocation);
        btnCalculateRoute = findViewById(R.id.btnCalculateRoute);
        etDestination = findViewById(R.id.etDestination);
        progressBar = findViewById(R.id.progressBar);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnGetCurrentLocation.setOnClickListener(v -> getCurrentLocation());
        btnCalculateRoute.setOnClickListener(v -> {
            String destination = etDestination.getText().toString().trim();
            if (destination.isEmpty()) {
                Toast.makeText(map_activity.this, "Please enter a destination", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentLocation == null) {
                Toast.makeText(map_activity.this, "Please get your current location first", Toast.LENGTH_SHORT).show();
                return;
            }

            // Convert destination address to coordinates
            geocodeDestination(destination);
        });
    }

    private void setupMap() {
        Configuration.getInstance().load(this,
                PreferenceManager.getDefaultSharedPreferences(this));

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(12.0);

        // Set default center (Nairobi coordinates)
        GeoPoint defaultCenter = new GeoPoint(-1.286389, 36.817223);
        mapView.getController().setCenter(defaultCenter);
    }

    private void setupBottomNavigation() {
        View navHome = findViewById(R.id.navHome);
        View navDirections = findViewById(R.id.navDirections);
        View navProfile = findViewById(R.id.navProfile);

        navHome.setOnClickListener(v -> {
            // Redirect to TripHistory activity
            navigateToTripHistory();
        });

        navDirections.setOnClickListener(v -> {
            if (lastRouteResult != null) {
                // Launch route details activity/screen
                launchRouteDetailsScreen();
            } else {
                Toast.makeText(this, "Calculate a route first", Toast.LENGTH_SHORT).show();
            }
        });

        navProfile.setOnClickListener(v -> {
            // Redirect to Profile activity
            navigateToProfile();
        });
    }

    private void navigateToTripHistory() {
        try {
            Log.d(TAG, "Navigating to TripHistory...");
            Intent intent = new Intent(map_activity.this, TripHistory.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to TripHistory: " + e.getMessage());
            Toast.makeText(this, "Cannot open trip history", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToProfile() {
        try {
            Log.d(TAG, "Navigating to Profile...");
            Intent intent = new Intent(map_activity.this, Profile.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to Profile: " + e.getMessage());
            Toast.makeText(this, "Cannot open profile", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchRouteDetailsScreen() {
        Intent intent = new Intent(this, RouteDetails.class);

        // Pass route information to the details screen
        if (lastRouteResult != null) {
            double distanceKm = lastRouteResult.distance / 1000;
            double timeMinutes = lastRouteResult.duration / 60;

            intent.putExtra("distance_km", distanceKm);
            intent.putExtra("time_minutes", timeMinutes);
            intent.putExtra("destination", lastDestination);
            intent.putExtra("route_points", serializeRoutePoints(lastRouteResult.routePoints));

            // Pass instructions if available
            if (lastRouteResult.instructions != null && !lastRouteResult.instructions.isEmpty()) {
                StringBuilder instructionsBuilder = new StringBuilder();
                for (RouteInstruction instruction : lastRouteResult.instructions) {
                    instructionsBuilder.append(instruction.text).append("|").append(instruction.distance).append(";");
                }
                intent.putExtra("instructions", instructionsBuilder.toString());
            }
        }

        startActivity(intent);
    }

    // Helper method to serialize route points for passing between activities
    private String serializeRoutePoints(List<GeoPoint> routePoints) {
        StringBuilder sb = new StringBuilder();
        for (GeoPoint point : routePoints) {
            sb.append(point.getLatitude()).append(",").append(point.getLongitude()).append(";");
        }
        return sb.toString();
    }

    private void clearMap() {
        mapView.getOverlays().clear();
        currentLocation = null;
        destinationLocation = null;
        lastRouteResult = null;
        mapView.invalidate();
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    progressBar.setVisibility(View.GONE);
                    if (location != null) {
                        currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());

                        // Center map on current location
                        mapView.getController().setCenter(currentLocation);

                        // Clear existing markers
                        mapView.getOverlays().clear();

                        // Add marker for current location
                        Marker currentLocationMarker = new Marker(mapView);
                        currentLocationMarker.setPosition(currentLocation);
                        currentLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        currentLocationMarker.setTitle("Current Location");
                        currentLocationMarker.setSnippet("Your starting point");
                        currentLocationMarker.setIcon(getResources().getDrawable(R.drawable.location_on_24px));
                        currentLocationMarker.getIcon().setTint(Color.BLUE);
                        mapView.getOverlays().add(currentLocationMarker);

                        Toast.makeText(map_activity.this, "Location found!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(map_activity.this, "Unable to get location. Please ensure location is enabled.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(map_activity.this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void geocodeDestination(String destination) {
        progressBar.setVisibility(View.VISIBLE);
        lastDestination = destination;

        // Use GraphHopper's Geocoding API to convert address to coordinates
        new GeocodeTask().execute(destination);
    }

    @SuppressWarnings("deprecation")
    private class GeocodeTask extends AsyncTask<String, Void, GeoPoint> {
        @Override
        protected GeoPoint doInBackground(String... params) {
            String address = params[0];

            try {
                // GraphHopper Geocoding API endpoint
                String urlString = "https://graphhopper.com/api/1/geocode" +
                        "?q=" + java.net.URLEncoder.encode(address, "UTF-8") +
                        "&key=" + GRAPHHOPPER_API_KEY +
                        "&limit=1" +
                        "&locale=en";

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // Parse response
                    Gson gson = new Gson();
                    JsonObject jsonResponse = gson.fromJson(response.toString(), JsonObject.class);

                    if (jsonResponse.has("hits") && jsonResponse.getAsJsonArray("hits").size() > 0) {
                        JsonObject hit = jsonResponse.getAsJsonArray("hits").get(0).getAsJsonObject();
                        JsonObject point = hit.getAsJsonObject("point");

                        double lat = point.get("lat").getAsDouble();
                        double lng = point.get("lng").getAsDouble();

                        return new GeoPoint(lat, lng);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Geocoding error: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(GeoPoint result) {
            progressBar.setVisibility(View.GONE);

            if (result != null) {
                destinationLocation = result;

                // Add destination marker
                Marker destinationMarker = new Marker(mapView);
                destinationMarker.setPosition(destinationLocation);
                destinationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                destinationMarker.setTitle("Destination");
                destinationMarker.setSnippet(etDestination.getText().toString());
                destinationMarker.setIcon(getResources().getDrawable(R.drawable.outline_add_location_alt_24));
                destinationMarker.getIcon().setTint(Color.RED);
                mapView.getOverlays().add(destinationMarker);

                // Fit map to show both markers
                mapView.getController().setZoom(10.0);
                mapView.invalidate();

                // Calculate route
                calculateRoute();
            } else {
                Toast.makeText(map_activity.this,
                        "Could not find location for '" + etDestination.getText().toString() +
                                "'. Please try a different address.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void calculateRoute() {
        if (currentLocation == null || destinationLocation == null) {
            Toast.makeText(this, "Both start and destination locations are required", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        // Update current location in case it changed
                        currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());

                        // Clear existing routes
                        mapView.getOverlays().removeIf(overlay -> overlay instanceof Polyline);

                        // Create waypoints list
                        List<GeoPoint> waypoints = new ArrayList<>();
                        waypoints.add(currentLocation);
                        waypoints.add(destinationLocation);

                        // Execute the AsyncTask
                        new GraphHopperRouteCalculator().execute(waypoints);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Inner class for route instructions
    private static class RouteInstruction {
        String text;
        double distance;

        RouteInstruction(String text, double distance) {
            this.text = text;
            this.distance = distance;
        }
    }

    private static class RouteResult {
        List<GeoPoint> routePoints;
        double distance;
        double duration;
        List<RouteInstruction> instructions;

        RouteResult(List<GeoPoint> routePoints, double distance, double duration, List<RouteInstruction> instructions) {
            this.routePoints = routePoints;
            this.distance = distance;
            this.duration = duration;
            this.instructions = instructions;
        }
    }

    @SuppressWarnings("deprecation")
    private class GraphHopperRouteCalculator extends AsyncTask<List<GeoPoint>, Void, RouteResult> {

        @Override
        protected RouteResult doInBackground(List<GeoPoint>... params) {
            if (params.length == 0 || params[0].size() < 2) return null;

            List<GeoPoint> waypoints = params[0];

            try {
                // Build GraphHopper API URL with instructions enabled
                StringBuilder pointsBuilder = new StringBuilder();
                for (GeoPoint point : waypoints) {
                    if (pointsBuilder.length() > 0) pointsBuilder.append("&");
                    pointsBuilder.append("point=")
                            .append(point.getLatitude())
                            .append(",")
                            .append(point.getLongitude());
                }

                String urlString = GRAPHHOPPER_BASE_URL + "?" + pointsBuilder.toString() +
                        "&vehicle=car" +
                        "&key=" + GRAPHHOPPER_API_KEY +
                        "&elevation=false" +
                        "&points_encoded=false" +
                        "&instructions=true" +  // Enable instructions
                        "&optimize=false";

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                // Read response
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // Parse GraphHopper JSON response
                    Gson gson = new Gson();
                    JsonObject jsonResponse = gson.fromJson(response.toString(), JsonObject.class);
                    if (jsonResponse.has("paths") && jsonResponse.getAsJsonArray("paths").size() > 0) {
                        JsonArray paths = jsonResponse.getAsJsonArray("paths");
                        JsonObject path = paths.get(0).getAsJsonObject();

                        double distance = path.get("distance").getAsDouble();
                        double time = path.get("time").getAsDouble();

                        JsonObject points = path.getAsJsonObject("points");
                        JsonArray coordinates = points.getAsJsonArray("coordinates");

                        List<GeoPoint> routePoints = new ArrayList<>();
                        for (int i = 0; i < coordinates.size(); i++) {
                            JsonArray coord = coordinates.get(i).getAsJsonArray();
                            double lon = coord.get(0).getAsDouble();
                            double lat = coord.get(1).getAsDouble();
                            routePoints.add(new GeoPoint(lat, lon));
                        }

                        // Parse instructions if available
                        List<RouteInstruction> instructions = new ArrayList<>();
                        if (path.has("instructions")) {
                            JsonArray instructionsArray = path.getAsJsonArray("instructions");
                            for (int i = 0; i < instructionsArray.size(); i++) {
                                JsonObject instruction = instructionsArray.get(i).getAsJsonObject();
                                String text = instruction.get("text").getAsString();
                                double instDistance = instruction.get("distance").getAsDouble();
                                instructions.add(new RouteInstruction(text, instDistance));
                            }
                        }

                        return new RouteResult(routePoints, distance, time / 1000, instructions);
                    }
                } else {
                    BufferedReader errorReader = new BufferedReader(
                            new InputStreamReader(connection.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    errorReader.close();
                    Log.e(TAG, "GraphHopper API Error " + responseCode + ": " + errorResponse.toString());
                }

            } catch (Exception e) {
                Log.e(TAG, "Error calculating route: " + e.getMessage());
            }

            return null;
        }

        @Override
        protected void onPostExecute(RouteResult result) {
            progressBar.setVisibility(View.GONE);

            if (result != null && !result.routePoints.isEmpty()) {
                drawRoute(result.routePoints);
                lastRouteResult = result; // Store for later use

                double distanceKm = result.distance / 1000;
                double timeMinutes = result.duration / 60;

                // Count number of instructions
                int instructionCount = result.instructions != null ? result.instructions.size() : 0;

                String successMessage = String.format("Route calculated: %.1f km, %.0f min, %d turns",
                        distanceKm, timeMinutes, instructionCount);
                Toast.makeText(map_activity.this, successMessage, Toast.LENGTH_LONG).show();

                // Show a prompt to view details
                Toast.makeText(map_activity.this,
                        "Tap 'Directions' in bottom nav to view turn-by-turn directions",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(map_activity.this,
                        "Failed to calculate route. Using straight line instead.", Toast.LENGTH_LONG).show();
                drawStraightLineFallback();
            }
        }
    }

    private void drawRoute(List<GeoPoint> routePoints) {
        Polyline routeLine = new Polyline();
        routeLine.setPoints(routePoints);
        routeLine.setColor(Color.argb(200, 76, 175, 80)); // Green color
        routeLine.setWidth(14.0f);
        mapView.getOverlays().add(routeLine);
        mapView.invalidate();
    }

    private void drawStraightLineFallback() {
        if (currentLocation != null && destinationLocation != null) {
            Polyline fallbackLine = new Polyline();
            fallbackLine.setPoints(List.of(currentLocation, destinationLocation));
            fallbackLine.setColor(0xFFFF9800); // Orange color
            fallbackLine.setWidth(8.0f);
            fallbackLine.setEnabled(true);
            mapView.getOverlays().add(fallbackLine);
            mapView.invalidate();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission is required for route planning", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDetach();
        }
    }
}