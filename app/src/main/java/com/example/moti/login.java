package com.example.moti;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;

public class login extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginButton;
    private OauthService oauthService;

    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        oauthService = new OauthService();

        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_btn);

        loginButton.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email & Password are required", Toast.LENGTH_SHORT).show();
            return;
        }

        oauthService.loginUser(email, password, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Login failed", e);
                runOnUiThread(() ->
                        Toast.makeText(login.this, "Network error", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body().string();
                Log.d(TAG, "Login Response: " + json);

                if (!response.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(login.this, "Invalid email or password", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                // Parse the JSON response
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

                // Extract token
                String token = jsonObject.get("token").getAsString();
                Log.d(TAG, "Token extracted: " + token.substring(0, Math.min(20, token.length())) + "...");

                // Extract user data
                JsonObject userObject = jsonObject.getAsJsonObject("user");
                String userEmail = userObject.get("email").getAsString();
                String motiId = userObject.get("moti_id").getAsString();

                runOnUiThread(() -> {
                    Toast.makeText(login.this, "Login successful!", Toast.LENGTH_SHORT).show();

                    // ✅ FIX: Save to the SAME SharedPreferences file that Profile reads from
                    getSharedPreferences("APP_PREFS", MODE_PRIVATE)
                            .edit()
                            .putString("auth_token", token)
                            .putString("user_email", userEmail)
                            .putString("moti_id", motiId)
                            .putBoolean("is_logged_in", true)
                            .apply();

                    Log.d(TAG, "Token saved to APP_PREFS");

                    // ✅ TEST: Verify it was saved
                    String savedToken = getSharedPreferences("APP_PREFS", MODE_PRIVATE)
                            .getString("auth_token", null);
                    Log.d(TAG, "Token verification: " + (savedToken != null ? "Saved successfully" : "NOT SAVED!"));

                    // Redirect to next activity
                    Intent intent = new Intent(login.this, map_activity.class);
                    startActivity(intent);
                    finish();
                });
            }
        });
    }
}