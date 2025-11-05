package com.example.moti;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 onboardingViewPager;
    private Button buttonNext, buttonBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        onboardingViewPager = findViewById(R.id.onboardingViewPager);
        buttonNext = findViewById(R.id.buttonNext);
        buttonBack = findViewById(R.id.buttonBack);

        setupOnboardingItems();

        buttonNext.setOnClickListener(v -> {
            if (onboardingViewPager.getCurrentItem() + 1 < 3) {
                onboardingViewPager.setCurrentItem(onboardingViewPager.getCurrentItem() + 1);
            } else {
                completeOnboarding();
            }
        });

        buttonBack.setOnClickListener(v -> {
            int currentItem = onboardingViewPager.getCurrentItem();
            if (currentItem > 0) {
                onboardingViewPager.setCurrentItem(currentItem - 1);
            } else {
                // Optional: exit or ignore if already at first page
                finish();
            }
        });
    }

    private void setupOnboardingItems() {
        List<OnboardingItem> items = new ArrayList<>();

        items.add(new OnboardingItem(
                R.drawable.one,
                "Welcome to Moti",
                "Get the Moti App and access your travel logs from home."
        ));
        items.add(new OnboardingItem(
                R.drawable.two,
                "Track Your Journey",
                "Monitor your every step within the day despite your travel model."
        ));
        items.add(new OnboardingItem(
                R.drawable.three,
                "Improve Road Safety",
                "Rate your driver or traffic experience and help improve road safety."
        ));

        onboardingViewPager.setAdapter(new OnboardingAdapter(items));
    }

    private void completeOnboarding() {
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isFirstRun", false);
        editor.apply();

        startActivity(new Intent(OnboardingActivity.this, MainActivity.class));
        finish();
    }
}
