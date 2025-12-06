package com.example.moti;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TripTracking extends AppCompatActivity {

    private static final String TAG = "TripTracking";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final int LOCATION_UPDATE_INTERVAL = 5000; // 5 seconds
    private static final int FASTEST_LOCATION_INTERVAL = 3000; // 3 seconds

    // UI Components
    private MapView mapView;
    private TextView tvDistanceRemaining, tvTimeRemaining, tvCurrentSpeed, tvElapsedTime;
    private TextView tvStatus, tvDestination;
    private Button btnEndTrip, btnViewHistory;
    private LinearLayout statsContainer;

    // Location Services
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    // Trip Data
    private String destination;
    private List<GeoPoint> routePoints;
    private double totalDistanceKm;
    private double estimatedTimeMinutes;
    private double distanceTraveled = 0;
    private long tripStartTime;
    private boolean isTripActive = true;

    // Trip Tracking Data
    private List<GeoPoint> traveledPath = new ArrayList<>();
    private GeoPoint currentLocation;
    private GeoPoint destinationLocation;
    private double averageSpeed = 0;
    private int locationUpdatesCount = 0;

    // Route visualization
    private Polyline originalRouteLine;
    private Polyline traveledRouteLine;
    private Marker currentLocationMarker;
    private Marker destinationMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_tracking);

        initializeViews();
        getTripDataFromIntent();
        setupMap();
        setupLocationUpdates();
        startTripTimer();
    }

    private void initializeViews() {
        mapView = findViewById(R.id.mapView);
        tvDistanceRemaining = findViewById(R.id.tvDistanceRemaining);
        tvTimeRemaining = findViewById(R.id.tvTimeRemaining);
        tvCurrentSpeed = findViewById(R.id.tvCurrentSpeed);
        tvElapsedTime = findViewById(R.id.tvElapsedTime);
        tvStatus = findViewById(R.id.tvStatus);
        tvDestination = findViewById(R.id.tvDestination);
        btnEndTrip = findViewById(R.id.btnEndTrip);
        btnViewHistory = findViewById(R.id.btnViewHistory);
        statsContainer = findViewById(R.id.statsContainer);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnEndTrip.setOnClickListener(v -> showEndTripConfirmation());

        // Set up View History button
        if (btnViewHistory != null) {
            btnViewHistory.setOnClickListener(v -> navigateToTripHistory());
        }
    }

    private void getTripDataFromIntent() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            destination = extras.getString("destination", "Unknown Destination");
            totalDistanceKm = extras.getDouble("distance_km", 0);
            estimatedTimeMinutes = extras.getDouble("time_minutes", 0);
            String routePointsString = extras.getString("route_points", "");

            // Parse route points
            routePoints = parseRoutePoints(routePointsString);
            if (routePoints.size() >= 2) {
                destinationLocation = routePoints.get(routePoints.size() - 1);
            }

            // Update UI with trip info
            tvDestination.setText("To: " + destination);
            tvStatus.setText("🚗 Trip in progress...");
            updateDistanceRemaining(totalDistanceKm);
            updateTimeRemaining(estimatedTimeMinutes);

            // Record start time for duration calculation
            tripStartTime = System.currentTimeMillis();
        }
    }

    private List<GeoPoint> parseRoutePoints(String routePointsString) {
        List<GeoPoint> points = new ArrayList<>();
        try {
            String[] pointStrings = routePointsString.split(";");
            for (String pointStr : pointStrings) {
                if (!pointStr.trim().isEmpty()) {
                    String[] coords = pointStr.split(",");
                    if (coords.length == 2) {
                        double lat = Double.parseDouble(coords[0]);
                        double lon = Double.parseDouble(coords[1]);
                        points.add(new GeoPoint(lat, lon));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing route points: " + e.getMessage());
        }
        return points;
    }

    private void setupMap() {
        Configuration.getInstance().load(this, getSharedPreferences("osm", MODE_PRIVATE));
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);

        if (routePoints != null && !routePoints.isEmpty()) {
            // Draw original route
            originalRouteLine = new Polyline();
            originalRouteLine.setPoints(routePoints);
            originalRouteLine.setColor(Color.argb(150, 66, 133, 244)); // Blue with transparency
            originalRouteLine.setWidth(10.0f);
            mapView.getOverlays().add(originalRouteLine);

            // Setup traveled route line
            traveledRouteLine = new Polyline();
            traveledRouteLine.setColor(Color.argb(200, 76, 175, 80)); // Green
            traveledRouteLine.setWidth(12.0f);
            mapView.getOverlays().add(traveledRouteLine);

            // Add destination marker
            destinationMarker = new Marker(mapView);
            destinationMarker.setPosition(destinationLocation);
            destinationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            destinationMarker.setTitle("Destination");
            destinationMarker.setIcon(getResources().getDrawable(R.drawable.outline_add_location_alt_24));
            destinationMarker.getIcon().setTint(Color.RED);
            mapView.getOverlays().add(destinationMarker);

            // Center on start point
            mapView.getController().setCenter(routePoints.get(0));
        }
    }

    @SuppressLint("MissingPermission")
    private void setupLocationUpdates() {
        if (checkLocationPermission()) {
            startLocationUpdates();
        } else {
            requestLocationPermission();
        }
    }

    private boolean checkLocationPermission() {
        return ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
                .setMinUpdateIntervalMillis(FASTEST_LOCATION_INTERVAL)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location location = locationResult.getLastLocation();
                if (location != null && isTripActive) {
                    handleNewLocation(location);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void handleNewLocation(Location location) {
        GeoPoint newPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        currentLocation = newPoint;
        traveledPath.add(newPoint);

        // Update current location marker
        updateCurrentLocationMarker(newPoint);

        // Update traveled route
        traveledRouteLine.setPoints(traveledPath);
        mapView.invalidate();

        // Calculate distance traveled
        if (traveledPath.size() > 1) {
            GeoPoint previousPoint = traveledPath.get(traveledPath.size() - 2);
            double segmentDistance = previousPoint.distanceToAsDouble(newPoint) / 1000; // in km
            distanceTraveled += segmentDistance;

            // Calculate remaining distance (simplified - using straight line to destination)
            double remainingDistance = calculateRemainingDistance(newPoint);
            updateDistanceRemaining(remainingDistance);

            // Calculate speed
            double speedKph = location.getSpeed() * 3.6; // Convert m/s to km/h
            updateCurrentSpeed(speedKph);

            // Update average speed
            locationUpdatesCount++;
            averageSpeed = ((averageSpeed * (locationUpdatesCount - 1)) + speedKph) / locationUpdatesCount;

            // Estimate remaining time
            double estimatedRemainingTime = calculateRemainingTime(remainingDistance, speedKph);
            updateTimeRemaining(estimatedRemainingTime);

            // Check if destination reached
            if (remainingDistance < 0.1) { // Within 100 meters
                tripCompleted();
            }
        } else {
            // First location update
            updateCurrentLocationMarker(newPoint);
        }
    }

    private double calculateRemainingDistance(GeoPoint currentPoint) {
        if (destinationLocation != null) {
            double directDistance = currentPoint.distanceToAsDouble(destinationLocation) / 1000;
            return Math.max(0, directDistance);
        }
        return Math.max(0, totalDistanceKm - distanceTraveled);
    }

    private double calculateRemainingTime(double remainingDistance, double currentSpeed) {
        if (currentSpeed > 1) { // If moving
            return (remainingDistance / currentSpeed) * 60; // Convert hours to minutes
        } else {
            return (remainingDistance / 30) * 60; // Assume average speed of 30 km/h if not moving
        }
    }

    private void updateCurrentLocationMarker(GeoPoint point) {
        mapView.getOverlays().removeIf(overlay -> overlay instanceof Marker &&
                ((Marker) overlay).getTitle() != null &&
                ((Marker) overlay).getTitle().equals("Current Location"));

        currentLocationMarker = new Marker(mapView);
        currentLocationMarker.setPosition(point);
        currentLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        currentLocationMarker.setTitle("Current Location");
        currentLocationMarker.setIcon(getResources().getDrawable(R.drawable.location_on_24px));
        currentLocationMarker.getIcon().setTint(Color.BLUE);
        mapView.getOverlays().add(currentLocationMarker);

        // Center map on current location
        mapView.getController().animateTo(point);
    }

    private void updateDistanceRemaining(double distanceKm) {
        tvDistanceRemaining.setText(String.format("Remaining: %.1f km", distanceKm));
    }

    private void updateTimeRemaining(double timeMinutes) {
        int hours = (int) (timeMinutes / 60);
        int minutes = (int) (timeMinutes % 60);

        if (hours > 0) {
            tvTimeRemaining.setText(String.format("ETA: %dh %dm", hours, minutes));
        } else {
            tvTimeRemaining.setText(String.format("ETA: %d min", minutes));
        }
    }

    private void updateCurrentSpeed(double speedKph) {
        tvCurrentSpeed.setText(String.format("Speed: %.0f km/h", speedKph));
    }

    private void updateElapsedTime(long elapsedMillis) {
        long hours = TimeUnit.MILLISECONDS.toHours(elapsedMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMillis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMillis) % 60;

        if (hours > 0) {
            tvElapsedTime.setText(String.format("Elapsed: %dh %dm", hours, minutes));
        } else {
            tvElapsedTime.setText(String.format("Elapsed: %dm %ds", minutes, seconds));
        }
    }

    private void startTripTimer() {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (isTripActive) {
                    long elapsedMillis = System.currentTimeMillis() - tripStartTime;
                    updateElapsedTime(elapsedMillis);
                    handler.postDelayed(this, 1000); // Update every second
                }
            }
        });
    }

    private void tripCompleted() {
        isTripActive = false;
        tvStatus.setText("✅ Trip Completed!");
        tvStatus.setTextColor(Color.GREEN);
        Toast.makeText(this, "You have reached your destination!", Toast.LENGTH_LONG).show();

        // Stop location updates
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        // Calculate final statistics
        long tripDuration = System.currentTimeMillis() - tripStartTime;
        double tripDurationMinutes = TimeUnit.MILLISECONDS.toMinutes(tripDuration);

        // Save trip to history
        saveCompletedTripToHistory(tripDurationMinutes, averageSpeed);

        // Show completion summary
        showTripSummary(tripDuration, averageSpeed);

        // Navigate to history after delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            navigateToTripHistory();
        }, 3000); // 3 second delay to show completion message
    }

    private void showEndTripConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("End Trip")
                .setMessage("Are you sure you want to end this trip?")
                .setPositiveButton("End Trip", (dialog, which) -> {
                    confirmEndTrip();
                })
                .setNegativeButton("Continue Trip", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void confirmEndTrip() {
        isTripActive = false;

        // Stop location updates
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        // Calculate final statistics
        long tripDuration = System.currentTimeMillis() - tripStartTime;
        double tripDurationMinutes = TimeUnit.MILLISECONDS.toMinutes(tripDuration);

        // Save trip data
        saveCompletedTripToHistory(tripDurationMinutes, averageSpeed);

        // Show summary
        showTripSummary(tripDuration, averageSpeed);

        // Navigate to TripHistory after short delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            navigateToTripHistory();
        }, 2000);
    }

    private void navigateToTripHistory() {
        Intent intent = new Intent(TripTracking.this, TripHistory.class);
        // Clear back stack so user goes to fresh history view
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Finish current activity
    }

    private void saveCompletedTripToHistory(double durationMinutes, double averageSpeedKph) {
        try {
            // Convert traveled path to route points string
            String routePointsString = convertPathToRouteString(traveledPath);

            // Ensure we have valid distance traveled
            double finalDistance = distanceTraveled > 0 ? distanceTraveled : totalDistanceKm;

            // Create a completed trip
            TripHistory.Trip completedTrip = new TripHistory.Trip(
                    destination,
                    finalDistance,
                    durationMinutes,
                    averageSpeedKph,
                    routePointsString
            );

            // Save to history
            TripHistory.saveTripToHistory(this, completedTrip);

            Log.d(TAG, "Trip saved to history: " + destination +
                    ", Distance: " + finalDistance + "km, Duration: " + durationMinutes + "min");

        } catch (Exception e) {
            Log.e(TAG, "Error saving trip to history: " + e.getMessage());
            Toast.makeText(this, "Error saving trip: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String convertPathToRouteString(List<GeoPoint> path) {
        StringBuilder routeBuilder = new StringBuilder();
        try {
            for (int i = 0; i < path.size(); i++) {
                GeoPoint point = path.get(i);
                routeBuilder.append(point.getLatitude())
                        .append(",")
                        .append(point.getLongitude());
                if (i < path.size() - 1) {
                    routeBuilder.append(";");
                }
            }
            return routeBuilder.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error converting path to string: " + e.getMessage());
            return "";
        }
    }

    private void showTripSummary(long tripDuration, double averageSpeed) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(tripDuration);
        double distanceKm = distanceTraveled > 0 ? distanceTraveled : totalDistanceKm;

        String summary = String.format(
                "✅ Trip Completed!\n\n" +
                        "Destination: %s\n" +
                        "Distance: %.1f km\n" +
                        "Duration: %d minutes\n" +
                        "Avg Speed: %.0f km/h\n\n" +
                        "Redirecting to Trip History...",
                destination, distanceKm, minutes, averageSpeed
        );

        // Update status text
        tvStatus.setText("Trip Completed!");
        tvStatus.setTextColor(Color.GREEN);

        Toast.makeText(this, summary, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission required for trip tracking", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
        // Stop location updates when paused (if trip is not active)
        if (fusedLocationClient != null && locationCallback != null && !isTripActive) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDetach();
        }
        // Ensure location updates are stopped
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}