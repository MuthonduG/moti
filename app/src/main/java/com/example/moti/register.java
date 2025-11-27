package com.example.moti;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class register extends AppCompatActivity {

    private EditText emailInput;
    private Button registerButton;
    private TextView signInText;

    private OauthService oauthService;
    private static final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        // Fix window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Init service
        oauthService = new OauthService();

        // Bind UI
        emailInput = findViewById(R.id.register_email_input);
        registerButton = findViewById(R.id.register_btn);
        signInText = findViewById(R.id.initial_sigin);

        // Go to login
        signInText.setOnClickListener(v -> {
            startActivity(new Intent(register.this, login.class));
        });

        // Register action
        registerButton.setOnClickListener(v -> {
            if (!isConnected()) {
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
                return;
            }

            String email = emailInput.getText().toString().trim();
            if (email.isEmpty()) {
                emailInput.setError("Email is required");
                return;
            }

            callRegisterApi(email);
        });
    }

    // ---------------------- REGISTER API CALL ----------------------
    private void callRegisterApi(String email) {

        oauthService.registerUser(email, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API Failed: " + e.getMessage());
                runOnUiThread(() ->
                        Toast.makeText(register.this, "Server connection failed", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    Log.e(TAG, "Register Failed: " + responseBody);
                    runOnUiThread(() ->
                            Toast.makeText(register.this, "Registration error", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                Log.d(TAG, "Register Success: " + responseBody);

                runOnUiThread(() -> {
                    Toast.makeText(register.this, "OTP sent to email", Toast.LENGTH_LONG).show();

                    // Move to verify screen
                    Intent intent = new Intent(register.this, otp_email_verification.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                });
            }
        });
    }

    // ---------------------- INTERNET CHECK ----------------------
    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }
}
