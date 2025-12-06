package com.example.moti;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class Profile extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private OauthService oauthService;
    private Gson gson = new Gson();

    private TextView tvTotalTrips, tvAvgRating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        oauthService = new OauthService();

        // -----------------------------
        // Initialize Views
        // -----------------------------
        tvTotalTrips = findViewById(R.id.tvTotalTrips);
        tvAvgRating = findViewById(R.id.tvAvgRating);

        MaterialButton btnBack = findViewById(R.id.btnBack);
        MaterialButton btnEditProfile = findViewById(R.id.btnEditProfile);
        MaterialButton btnSettings = findViewById(R.id.btnSettings);
        MaterialButton btnLogout = findViewById(R.id.btnLogout);

        CardView cardTrips = findViewById(R.id.cardTrips);
        CardView cardRating = findViewById(R.id.cardRating);
        ImageView ivProfile = findViewById(R.id.ivProfile);

        // -----------------------------
        // Load stats
        // -----------------------------
        updateProfileStats(tvTotalTrips, tvAvgRating);

        // -----------------------------
        // Get the saved token
        // -----------------------------
        String token = getSharedPreferences("APP_PREFS", MODE_PRIVATE)
                .getString("auth_token", null);

        if (token == null) {
            Toast.makeText(this, "Session expired, please log in again", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Profile.this, login.class));
            finish();
            return;
        }

        Log.d(TAG, "Loaded token from SharedPreferences");

        // -----------------------------
        // Fetch user info from API
        // -----------------------------
        fetchUser(token);

        // -----------------------------
        // Button listeners
        // -----------------------------
        btnBack.setOnClickListener(v -> finish());

        btnEditProfile.setOnClickListener(v ->
                Toast.makeText(this, "Edit Profile", Toast.LENGTH_SHORT).show()
        );

        btnSettings.setOnClickListener(v ->
                Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
        );

        btnLogout.setOnClickListener(v -> showLogoutConfirmation());

        cardTrips.setOnClickListener(v ->
                startActivity(new Intent(Profile.this, TripHistory.class))
        );

        cardRating.setOnClickListener(v ->
                Toast.makeText(this, "Rated Trips", Toast.LENGTH_SHORT).show()
        );

        ivProfile.setOnClickListener(v ->
                Toast.makeText(this, "Change Profile Photo", Toast.LENGTH_SHORT).show()
        );
    }


    // ---------------------------------------------------------
    // FETCH USER DATA WITH TOKEN
    // ---------------------------------------------------------
    private void fetchUser(String token) {

        oauthService.getUser(token, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "GetUser failed", e);
                runOnUiThread(() ->
                        Toast.makeText(Profile.this, "Network error", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String json = response.body().string();
                Log.d(TAG, "GetUser Response: " + json);

                if (!response.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(Profile.this, "Could not load user info", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                // Parse JSON
                JsonObject obj = gson.fromJson(json, JsonObject.class);
                JsonObject userObj = obj.getAsJsonObject("user");

                String email = userObj.get("email").getAsString();
                String motiId = userObj.get("moti_id").getAsString();

                // Update UI
                runOnUiThread(() -> {
                    Toast.makeText(Profile.this,
                            "Logged in as: " + email,
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    // ---------------------------------------------------------
    // UPDATE TRIP STATS
    // ---------------------------------------------------------
    private void updateProfileStats(TextView tvTotalTrips, TextView tvAvgRating) {
        try {
            java.util.List<TripHistory.Trip> trips = TripHistory.getTripHistory(this);
            int totalTrips = trips.size();

            float totalRating = 0;
            int ratedTrips = 0;

            for (TripHistory.Trip trip : trips) {
                if (trip.getRating() > 0) {
                    totalRating += trip.getRating();
                    ratedTrips++;
                }
            }

            float avgRating = ratedTrips > 0 ? totalRating / ratedTrips : 0;

            tvTotalTrips.setText(String.valueOf(totalTrips));
            tvAvgRating.setText(String.format("%.1f", avgRating));

        } catch (Exception e) {
            Log.e(TAG, "Error calculating stats", e);
            tvTotalTrips.setText("0");
            tvAvgRating.setText("0.0");
        }
    }

    // ---------------------------------------------------------
    // LOGOUT CONFIRMATION
    // ---------------------------------------------------------
    private void showLogoutConfirmation() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    getSharedPreferences("APP_PREFS", MODE_PRIVATE)
                            .edit()
                            .clear()
                            .apply();

                    Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Profile.this, login.class));
                    finishAffinity();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh stats only
        updateProfileStats(tvTotalTrips, tvAvgRating);
    }
}
