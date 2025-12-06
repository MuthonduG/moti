package com.example.moti;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

public class RouteDetails extends AppCompatActivity {

    private TextView tvDistance, tvTime, tvDestination;
    private MapView miniMapView;
    private Button btnStartTrip;

    private String routePointsString;
    private String destination;
    private double distanceKm;
    private double timeMinutes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_details);

        tvDistance = findViewById(R.id.tvDistance);
        tvTime = findViewById(R.id.tvTime);
        tvDestination = findViewById(R.id.tvDestination);
        miniMapView = findViewById(R.id.miniMapView);
        btnStartTrip = findViewById(R.id.btnStartTrip);

        // Configure mini map
        setupMiniMap();

        // Get data from intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            distanceKm = extras.getDouble("distance_km", 0);
            timeMinutes = extras.getDouble("time_minutes", 0);
            destination = extras.getString("destination", "");
            routePointsString = extras.getString("route_points", "");

            // Display route information
            displayRouteInfo(distanceKm, timeMinutes, destination);

            // Display route on mini map
            if (routePointsString != null && !routePointsString.isEmpty()) {
                displayRouteOnMap(routePointsString);
            }
        }

        // Set up start trip button
        btnStartTrip.setOnClickListener(v -> startTrip());
    }

    private void setupMiniMap() {
        Configuration.getInstance().load(this, getSharedPreferences("osm", MODE_PRIVATE));

        miniMapView.setTileSource(TileSourceFactory.MAPNIK);
        miniMapView.setMultiTouchControls(false); // Disable zoom/pan for mini map
        miniMapView.setClickable(false);
        miniMapView.setBuiltInZoomControls(false);
        miniMapView.getController().setZoom(13.0);

        // Set map to show entire Nairobi by default
        GeoPoint nairobiCenter = new GeoPoint(-1.286389, 36.817223);
        miniMapView.getController().setCenter(nairobiCenter);
    }

    private void displayRouteOnMap(String routePointsString) {
        try {
            List<GeoPoint> routePoints = parseRoutePoints(routePointsString);

            if (routePoints.size() >= 2) {
                // Clear existing overlays
                miniMapView.getOverlays().clear();

                // Add start marker
                GeoPoint startPoint = routePoints.get(0);
                Marker startMarker = new Marker(miniMapView);
                startMarker.setPosition(startPoint);
                startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                startMarker.setTitle("Start");
                startMarker.setIcon(getResources().getDrawable(R.drawable.location_on_24px));
                startMarker.getIcon().setTint(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
                miniMapView.getOverlays().add(startMarker);

                // Add end marker
                GeoPoint endPoint = routePoints.get(routePoints.size() - 1);
                Marker endMarker = new Marker(miniMapView);
                endMarker.setPosition(endPoint);
                endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                endMarker.setTitle("Destination");
                endMarker.setIcon(getResources().getDrawable(R.drawable.outline_add_location_alt_24));
                endMarker.getIcon().setTint(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                miniMapView.getOverlays().add(endMarker);

                // Add route line
                Polyline routeLine = new Polyline();
                routeLine.setPoints(routePoints);
                routeLine.setColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                routeLine.setWidth(8.0f);
                miniMapView.getOverlays().add(routeLine);

                // Fit map to show entire route
                zoomToRoute(routePoints);

                miniMapView.invalidate();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Could not display route on map", Toast.LENGTH_SHORT).show();
        }
    }

    private void zoomToRoute(List<GeoPoint> routePoints) {
        if (routePoints.size() < 2) return;

        // Calculate bounds
        double minLat = routePoints.get(0).getLatitude();
        double maxLat = routePoints.get(0).getLatitude();
        double minLon = routePoints.get(0).getLongitude();
        double maxLon = routePoints.get(0).getLongitude();

        for (GeoPoint point : routePoints) {
            minLat = Math.min(minLat, point.getLatitude());
            maxLat = Math.max(maxLat, point.getLatitude());
            minLon = Math.min(minLon, point.getLongitude());
            maxLon = Math.max(maxLon, point.getLongitude());
        }

        // Add padding
        double latPadding = (maxLat - minLat) * 0.2;
        double lonPadding = (maxLon - minLon) * 0.2;

        minLat -= latPadding;
        maxLat += latPadding;
        minLon -= lonPadding;
        maxLon += lonPadding;

        // Calculate center
        GeoPoint center = new GeoPoint(
                (minLat + maxLat) / 2,
                (minLon + maxLon) / 2
        );

        // Set zoom level based on bounds
        IMapController mapController = miniMapView.getController();
        mapController.setCenter(center);

        // Simple zoom calculation
        double latDiff = maxLat - minLat;
        double lonDiff = maxLon - minLon;
        double maxDiff = Math.max(latDiff, lonDiff);

        // Adjust zoom level based on area covered
        if (maxDiff > 0.5) {
            mapController.setZoom(10.0);
        } else if (maxDiff > 0.2) {
            mapController.setZoom(12.0);
        } else if (maxDiff > 0.1) {
            mapController.setZoom(14.0);
        } else {
            mapController.setZoom(16.0);
        }
    }

    private List<GeoPoint> parseRoutePoints(String routePointsString) {
        List<GeoPoint> routePoints = new ArrayList<>();

        try {
            String[] pointStrings = routePointsString.split(";");
            for (String pointStr : pointStrings) {
                if (!pointStr.trim().isEmpty()) {
                    String[] coords = pointStr.split(",");
                    if (coords.length == 2) {
                        double lat = Double.parseDouble(coords[0]);
                        double lon = Double.parseDouble(coords[1]);
                        routePoints.add(new GeoPoint(lat, lon));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return routePoints;
    }

    private void displayRouteInfo(double distanceKm, double timeMinutes, String destination) {
        tvDistance.setText(String.format("Distance: %.1f km", distanceKm));
        tvTime.setText(String.format("Estimated Time: %.0f minutes", timeMinutes));
        tvDestination.setText(String.format("To: %s", destination));
    }

    private void startTrip() {
        // Launch trip tracking activity
        Intent tripIntent = new Intent(this, TripTracking.class);
        tripIntent.putExtra("destination", destination);
        tripIntent.putExtra("route_points", routePointsString);
        tripIntent.putExtra("distance_km", distanceKm);
        tripIntent.putExtra("time_minutes", timeMinutes);
        startActivity(tripIntent);
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (miniMapView != null) {
            miniMapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (miniMapView != null) {
            miniMapView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (miniMapView != null) {
            miniMapView.onDetach();
        }
    }
}