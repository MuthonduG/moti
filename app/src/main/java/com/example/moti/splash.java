package com.example.moti;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class splash extends AppCompatActivity {
    Handler handler = new Handler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets)-> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                }
        );

        handler.postDelayed(() -> {
            SharedPreferences preferences = getSharedPreferences("Preferences", MODE_PRIVATE);
            boolean isFirstRun = preferences.getBoolean("isFirstRun", true);

            if(isFirstRun) {
                startActivity(new Intent(splash.this, OnboardingActivity.class));
            } else {
                startActivity(new Intent(splash.this, MainActivity.class));
            }
            finish();
        }, 3000);
    }
}
