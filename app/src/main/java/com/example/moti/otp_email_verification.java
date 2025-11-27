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

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class otp_email_verification extends AppCompatActivity {

    private EditText emailField, otp1, otp2, otp3, otp4, otp5, otp6;
    private Button verifyBtn;
    private OauthService oauthService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_otp_email_verification);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        oauthService = new OauthService();

        emailField = findViewById(R.id.email_verification_input);
        otp1 = findViewById(R.id.otp_1);
        otp2 = findViewById(R.id.otp_2);
        otp3 = findViewById(R.id.otp_3);
        otp4 = findViewById(R.id.otp_4);
        otp5 = findViewById(R.id.otp_5);
        otp6 = findViewById(R.id.otp_6);
        verifyBtn = findViewById(R.id.verify_email_btn);

        // Pull email passed from register screen
        String email = getIntent().getStringExtra("email");
        if (email != null) {
            emailField.setText(email);
        }

        verifyBtn.setOnClickListener(v -> verifyEmail());
    }

    private void verifyEmail() {
        String email = emailField.getText().toString().trim();
        String otp_code = otp1.getText().toString() +
                otp2.getText().toString() +
                otp3.getText().toString() +
                otp4.getText().toString() +
                otp5.getText().toString() +
                otp6.getText().toString();

        if (email.isEmpty()) {
            Toast.makeText(this, "Email is required!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (otp_code.length() != 6) {
            Toast.makeText(this, "Enter a valid 6-digit OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        oauthService.verifyEmail(email, otp_code, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(otp_email_verification.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                Log.d("OTP_RESPONSE", body);

                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(otp_email_verification.this, "Email Verified!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(otp_email_verification.this, email_verification.class);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(otp_email_verification.this,
                            "Verification failed: " + body, Toast.LENGTH_LONG).show());
                }
            }
        });
    }
}
