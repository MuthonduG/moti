package com.example.moti;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TripHistory extends AppCompatActivity {

    // Trip data model class (inner class)
    public static class Trip implements Serializable {
        private String id;
        private String destination;
        private double distanceKm;
        private double durationMinutes;
        private double averageSpeedKph;
        private String startTime;
        private String endTime;
        private String routePoints;
        private float rating = 0.0f; // New: Rating from 0-5 stars
        private String notes = ""; // New: Optional notes

        // Constructor for completed trip
        public Trip(String destination, double distanceKm, double durationMinutes,
                    double averageSpeedKph, String routePoints) {
            this.id = generateId();
            this.destination = destination;
            this.distanceKm = distanceKm;
            this.durationMinutes = durationMinutes;
            this.averageSpeedKph = averageSpeedKph;
            this.startTime = getCurrentDateTime();
            this.endTime = getCurrentDateTime();
            this.routePoints = routePoints;
        }

        // Constructor with all parameters
        public Trip(String destination, double distanceKm, double durationMinutes,
                    double averageSpeedKph, String routePoints, String startTime,
                    String endTime, float rating, String notes) {
            this.id = generateId();
            this.destination = destination;
            this.distanceKm = distanceKm;
            this.durationMinutes = durationMinutes;
            this.averageSpeedKph = averageSpeedKph;
            this.startTime = startTime;
            this.endTime = endTime;
            this.routePoints = routePoints;
            this.rating = rating;
            this.notes = notes;
        }

        private String generateId() {
            return "TRIP_" + System.currentTimeMillis();
        }

        private String getCurrentDateTime() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return sdf.format(new Date());
        }

        // Getters and Setters
        public String getId() { return id; }
        public String getDestination() { return destination; }
        public double getDistanceKm() { return distanceKm; }
        public double getDurationMinutes() { return durationMinutes; }
        public double getAverageSpeedKph() { return averageSpeedKph; }
        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }
        public String getRoutePoints() { return routePoints; }
        public float getRating() { return rating; }
        public String getNotes() { return notes; }

        public void setEndTime(String endTime) { this.endTime = endTime; }
        public void setDurationMinutes(double durationMinutes) { this.durationMinutes = durationMinutes; }
        public void setAverageSpeedKph(double averageSpeedKph) { this.averageSpeedKph = averageSpeedKph; }
        public void setRating(float rating) { this.rating = rating; }
        public void setNotes(String notes) { this.notes = notes; }

        // Format duration for display
        public String getFormattedDuration() {
            int hours = (int) (durationMinutes / 60);
            int minutes = (int) (durationMinutes % 60);

            if (hours > 0) {
                return String.format("%dh %dm", hours, minutes);
            } else {
                return String.format("%d min", minutes);
            }
        }

        // Format date for display
        public String getFormattedDate() {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date date = sdf.parse(startTime);
                SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
                return displayFormat.format(date);
            } catch (Exception e) {
                return startTime;
            }
        }

        // Get formatted time only (HH:mm)
        public String getFormattedTime() {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date date = sdf.parse(startTime);
                SimpleDateFormat displayFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                return displayFormat.format(date);
            } catch (Exception e) {
                return startTime;
            }
        }

        // Get formatted date only (MMM dd)
        public String getFormattedDateOnly() {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date date = sdf.parse(startTime);
                SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
                return displayFormat.format(date);
            } catch (Exception e) {
                return startTime;
            }
        }
    }

    // Trip history manager (inner class)
    public static class TripHistoryManager {
        private static final String PREFS_NAME = "TripHistoryPrefs";
        private static final String TRIP_HISTORY_KEY = "trip_history";
        private static final int MAX_TRIPS = 10; // Increased from 5 to 10

        private SharedPreferences prefs;
        private Gson gson;

        public TripHistoryManager(Context context) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            gson = new Gson();
        }

        public void saveTrip(Trip trip) {
            List<Trip> trips = getTripHistory();

            // Add new trip at the beginning (most recent first)
            trips.add(0, trip);

            // Keep only the last MAX_TRIPS trips
            if (trips.size() > MAX_TRIPS) {
                trips = trips.subList(0, MAX_TRIPS);
            }

            String tripsJson = gson.toJson(trips);
            prefs.edit().putString(TRIP_HISTORY_KEY, tripsJson).apply();
        }

        public void updateTrip(Trip updatedTrip) {
            List<Trip> trips = getTripHistory();

            for (int i = 0; i < trips.size(); i++) {
                if (trips.get(i).getId().equals(updatedTrip.getId())) {
                    trips.set(i, updatedTrip);
                    break;
                }
            }

            String tripsJson = gson.toJson(trips);
            prefs.edit().putString(TRIP_HISTORY_KEY, tripsJson).apply();
        }

        public List<Trip> getTripHistory() {
            String tripsJson = prefs.getString(TRIP_HISTORY_KEY, "");

            if (tripsJson.isEmpty()) {
                return new ArrayList<>();
            }

            Type type = new TypeToken<ArrayList<Trip>>(){}.getType();
            List<Trip> trips = gson.fromJson(tripsJson, type);

            if (trips == null) {
                return new ArrayList<>();
            }

            return trips;
        }

        public void clearTripHistory() {
            prefs.edit().remove(TRIP_HISTORY_KEY).apply();
        }

        public int getTripCount() {
            return getTripHistory().size();
        }
    }

    // Activity UI components
    private LinearLayout tripsContainer;
    private TextView tvEmptyState;
    private TextView tvSubtitle;
    private TripHistoryManager historyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_history); // Use XML layout

        // Initialize views
        tripsContainer = findViewById(R.id.tripsContainer);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        // Find subtitle text
        tvSubtitle = findViewById(R.id.tvSubtitle);
        if (tvSubtitle == null) {
            // If not in XML, create it programmatically
            tvSubtitle = new TextView(this);
            tvSubtitle.setText("Recent Trips");
            tvSubtitle.setTextSize(16);
            tvSubtitle.setTextColor(Color.GRAY);
            tvSubtitle.setPadding(0, 0, 0, 16);
            tripsContainer.addView(tvSubtitle, 0);
        }

        // Initialize buttons
        MaterialButton btnBack = findViewById(R.id.btnBack);
        MaterialButton btnClearHistory = findViewById(R.id.btnClearHistory);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnClearHistory != null) {
            btnClearHistory.setOnClickListener(v -> clearTripHistory());
        }

        historyManager = new TripHistoryManager(this);
        loadTripHistory();
    }

    private void loadTripHistory() {
        List<Trip> trips = historyManager.getTripHistory();

        if (trips.isEmpty()) {
            showEmptyState();
            return;
        }

        hideEmptyState();

        // Clear existing views except subtitle
        int startIndex = 1; // Skip subtitle at index 0
        if (tripsContainer.getChildCount() > startIndex) {
            tripsContainer.removeViews(startIndex, tripsContainer.getChildCount() - startIndex);
        }

        // Update subtitle with trip count
        tvSubtitle.setText("Recent Trips (" + trips.size() + ")");

        // Add trip cards
        for (int i = 0; i < trips.size(); i++) {
            Trip trip = trips.get(i);
            addTripCard(trip, i + 1);
        }
    }

    private void addTripCard(Trip trip, int position) {
        CardView tripCard = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 16);
        tripCard.setLayoutParams(cardParams);
        tripCard.setCardBackgroundColor(Color.WHITE);
        tripCard.setRadius(16);
        tripCard.setCardElevation(6);
        tripCard.setContentPadding(20, 20, 20, 20);

        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(LinearLayout.VERTICAL);

        // Header row (Position and Date)
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView tvPosition = new TextView(this);
        tvPosition.setText("#" + position);
        tvPosition.setTextSize(16);
        tvPosition.setTextColor(ContextCompat.getColor(this, R.color.blue_700));
        tvPosition.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvDate = new TextView(this);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        dateParams.setMargins(8, 0, 0, 0);
        tvDate.setLayoutParams(dateParams);
        tvDate.setText(" • " + trip.getFormattedDate());
        tvDate.setTextSize(14);
        tvDate.setTextColor(Color.GRAY);

        headerRow.addView(tvPosition);
        headerRow.addView(tvDate);
        cardContent.addView(headerRow);

        // Destination
        TextView tvDestination = new TextView(this);
        tvDestination.setText("📍 " + trip.getDestination());
        tvDestination.setTextSize(18);
        tvDestination.setTextColor(Color.BLACK);
        tvDestination.setPadding(0, 8, 0, 12);
        tvDestination.setTypeface(null, android.graphics.Typeface.BOLD);
        cardContent.addView(tvDestination);

        // Stats row
        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        // Distance
        LinearLayout distanceLayout = createStatLayout(
                "📏",
                String.format("%.1f km", trip.getDistanceKm()),
                "Distance"
        );

        // Duration
        LinearLayout durationLayout = createStatLayout(
                "⏱️",
                trip.getFormattedDuration(),
                "Duration"
        );

        // Speed
        LinearLayout speedLayout = createStatLayout(
                "🚀",
                String.format("%.0f km/h", trip.getAverageSpeedKph()),
                "Avg Speed"
        );

        LinearLayout.LayoutParams statParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        statParams.setMargins(4, 0, 4, 0);
        distanceLayout.setLayoutParams(statParams);
        durationLayout.setLayoutParams(statParams);
        speedLayout.setLayoutParams(statParams);

        statsRow.addView(distanceLayout);
        statsRow.addView(durationLayout);
        statsRow.addView(speedLayout);
        cardContent.addView(statsRow);

        // Rating Section
        LinearLayout ratingLayout = new LinearLayout(this);
        ratingLayout.setOrientation(LinearLayout.VERTICAL);
        ratingLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        ratingLayout.setPadding(0, 16, 0, 8);

        TextView tvRatingLabel = new TextView(this);
        tvRatingLabel.setText("Rate this trip:");
        tvRatingLabel.setTextSize(14);
        tvRatingLabel.setTextColor(Color.GRAY);
        ratingLayout.addView(tvRatingLabel);

        // Rating Bar
        LinearLayout ratingBarLayout = new LinearLayout(this);
        ratingBarLayout.setOrientation(LinearLayout.HORIZONTAL);
        ratingBarLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        RatingBar ratingBar = new RatingBar(this, null, android.R.attr.ratingBarStyleSmall);
        ratingBar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        ratingBar.setStepSize(1.0f);
        ratingBar.setNumStars(5);
        ratingBar.setRating(trip.getRating());
        ratingBar.setIsIndicator(false); // Allow user to change rating

        // Save rating when changed
        ratingBar.setOnRatingBarChangeListener((ratingBar1, rating, fromUser) -> {
            if (fromUser) {
                trip.setRating(rating);
                historyManager.updateTrip(trip);
                Toast.makeText(TripHistory.this, "Rating saved: " + rating + " stars", Toast.LENGTH_SHORT).show();
            }
        });

        TextView tvRatingValue = new TextView(this);
        tvRatingValue.setText(String.format("(%.1f)", trip.getRating()));
        tvRatingValue.setTextSize(14);
        tvRatingValue.setTextColor(ContextCompat.getColor(this, R.color.blue_700));
        tvRatingValue.setPadding(8, 0, 0, 0);
        tvRatingValue.setTypeface(null, android.graphics.Typeface.BOLD);

        ratingBarLayout.addView(ratingBar);
        ratingBarLayout.addView(tvRatingValue);
        ratingLayout.addView(ratingBarLayout);
        cardContent.addView(ratingLayout);

        // Action Buttons Row
        LinearLayout actionButtonsRow = new LinearLayout(this);
        actionButtonsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionButtonsRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        actionButtonsRow.setPadding(0, 8, 0, 0);

        // View Details Button
        TextView btnViewDetails = new TextView(this);
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        detailsParams.setMargins(0, 0, 8, 0);
        btnViewDetails.setLayoutParams(detailsParams);
        btnViewDetails.setText("VIEW DETAILS");
        btnViewDetails.setTextSize(14);
        btnViewDetails.setTextColor(ContextCompat.getColor(this, R.color.green_700));
        btnViewDetails.setTypeface(null, android.graphics.Typeface.BOLD);
        btnViewDetails.setGravity(android.view.Gravity.CENTER);
        btnViewDetails.setPadding(12, 8, 12, 8);
        btnViewDetails.setBackgroundResource(R.drawable.rounded_corner_green_light);
        btnViewDetails.setOnClickListener(v -> viewTripDetails(trip));

        // Add Notes Button
        TextView btnAddNotes = new TextView(this);
        LinearLayout.LayoutParams notesParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        notesParams.setMargins(8, 0, 0, 0);
        btnAddNotes.setLayoutParams(notesParams);
        btnAddNotes.setText("ADD NOTES");
        btnAddNotes.setTextSize(14);
        btnAddNotes.setTextColor(ContextCompat.getColor(this, R.color.blue_700));
        btnAddNotes.setTypeface(null, android.graphics.Typeface.BOLD);
        btnAddNotes.setGravity(android.view.Gravity.CENTER);
        btnAddNotes.setPadding(12, 8, 12, 8);
        btnAddNotes.setBackgroundResource(R.drawable.rounded_corner_blue_light);
        btnAddNotes.setOnClickListener(v -> addNotesToTrip(trip));

        actionButtonsRow.addView(btnViewDetails);
        actionButtonsRow.addView(btnAddNotes);
        cardContent.addView(actionButtonsRow);

        tripCard.addView(cardContent);
        tripsContainer.addView(tripCard);
    }

    private LinearLayout createStatLayout(String icon, String value, String label) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);
        layout.setPadding(8, 8, 8, 8);
        layout.setBackgroundResource(R.drawable.rounded_corner_white);

        TextView tvIcon = new TextView(this);
        tvIcon.setText(icon);
        tvIcon.setTextSize(20);
        tvIcon.setGravity(android.view.Gravity.CENTER);

        TextView tvValue = new TextView(this);
        tvValue.setText(value);
        tvValue.setTextSize(16);
        tvValue.setTextColor(Color.BLACK);
        tvValue.setTypeface(null, android.graphics.Typeface.BOLD);
        tvValue.setGravity(android.view.Gravity.CENTER);
        tvValue.setPadding(0, 4, 0, 2);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextSize(12);
        tvLabel.setTextColor(Color.GRAY);
        tvLabel.setGravity(android.view.Gravity.CENTER);

        layout.addView(tvIcon);
        layout.addView(tvValue);
        layout.addView(tvLabel);

        return layout;
    }

    private void viewTripDetails(Trip trip) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Trip Details - " + trip.getDestination());

        String details = String.format(
                "📍 Destination: %s\n\n" +
                        "📏 Distance: %.1f km\n" +
                        "⏱️ Duration: %s\n" +
                        "🚀 Average Speed: %.0f km/h\n" +
                        "⭐ Rating: %.1f / 5\n" +
                        "📅 Started: %s\n" +
                        "🏁 Ended: %s\n\n" +
                        "📝 Notes: %s",
                trip.getDestination(),
                trip.getDistanceKm(),
                trip.getFormattedDuration(),
                trip.getAverageSpeedKph(),
                trip.getRating(),
                trip.getStartTime(),
                trip.getEndTime(),
                trip.getNotes().isEmpty() ? "No notes added" : trip.getNotes()
        );

        builder.setMessage(details);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());

        // Add edit notes option
        builder.setNeutralButton("Edit Notes", (dialog, which) -> {
            addNotesToTrip(trip);
        });

        builder.show();
    }

    private void addNotesToTrip(Trip trip) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Add Notes for Trip to " + trip.getDestination());

        // Create EditText for notes
        android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setLines(4);
        input.setMaxLines(8);
        input.setHint("Enter your notes here...");
        input.setText(trip.getNotes());

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(32, 16, 32, 16);
        input.setLayoutParams(params);
        container.addView(input);

        builder.setView(container);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String notes = input.getText().toString().trim();
            trip.setNotes(notes);
            historyManager.updateTrip(trip);
            Toast.makeText(this, "Notes saved!", Toast.LENGTH_SHORT).show();
            loadTripHistory(); // Refresh to show updated notes
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void clearTripHistory() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Clear History")
                .setMessage("Are you sure you want to clear all trip history? This action cannot be undone.")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    historyManager.clearTripHistory();
                    // Clear all trip cards (keep subtitle)
                    if (tripsContainer.getChildCount() > 1) {
                        tripsContainer.removeViews(1, tripsContainer.getChildCount() - 1);
                    }
                    showEmptyState();
                    Toast.makeText(this, "Trip history cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEmptyState() {
        tvEmptyState.setVisibility(View.VISIBLE);
        tripsContainer.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        tvEmptyState.setVisibility(View.GONE);
        tripsContainer.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTripHistory(); // Refresh when returning to activity
    }

    // Static method to save a trip from other activities
    public static void saveTripToHistory(Context context, Trip trip) {
        TripHistoryManager manager = new TripHistoryManager(context);
        manager.saveTrip(trip);
    }

    // Static method to get trip history from other activities
    public static List<Trip> getTripHistory(Context context) {
        TripHistoryManager manager = new TripHistoryManager(context);
        return manager.getTripHistory();
    }

    // Static method to clear trip history from other activities
    public static void clearTripHistory(Context context) {
        TripHistoryManager manager = new TripHistoryManager(context);
        manager.clearTripHistory();
    }

    // Static method to add a sample trip (for testing)
    public static void addSampleTrip(Context context) {
        Trip sampleTrip = new Trip(
                "Sample Destination",
                15.5,
                45.0,
                60.0,
                "-1.286389,36.817223;-1.2921,36.8219"
        );
        sampleTrip.setRating(4.5f);
        sampleTrip.setNotes("Great trip! Traffic was light.");
        saveTripToHistory(context, sampleTrip);
    }
}