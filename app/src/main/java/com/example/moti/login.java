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

                LoginResponse loginResponse = new Gson().fromJson(json, LoginResponse.class);

                runOnUiThread(() -> {
                    Toast.makeText(login.this, "Login successful!", Toast.LENGTH_SHORT).show();

                    getSharedPreferences("auth", MODE_PRIVATE)
                            .edit()
                            .putString("token", loginResponse.token)
                            .apply();

                    // Redirect to next activity
                    Intent intent = new Intent(login.this, map_activity.class);
                    startActivity(intent);
                    finish();
                });
            }
        });
    }

    private static class LoginResponse {
        String token;
        String message;
        String user_id;
    }
}
