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
    private Button buttonNext, buttonSkip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        onboardingViewPager = findViewById(R.id.onboardingViewPager);
        buttonNext = findViewById(R.id.buttonNext);
        buttonSkip = findViewById(R.id.buttonSkip);

        setupOnboardingItems();

        buttonNext.setOnClickListener(v -> {
            if (onboardingViewPager.getCurrentItem() + 1 < 3) {
                onboardingViewPager.setCurrentItem(onboardingViewPager.getCurrentItem() + 1);
            } else {
                completeOnboarding();
            }
        });

        buttonSkip.setOnClickListener(v -> completeOnboarding());
    }

    private void setupOnboardingItems() {
        List<OnboardingItem> items = new ArrayList<>();

        items.add(new OnboardingItem(
                R.drawable.one,
                "Welcome to Moti",
                "Stay motivated and organized every day."
        ));
        items.add(new OnboardingItem(
                R.drawable.two,
                "Track Progress",
                "Monitor your goals and accomplishments."
        ));
        items.add(new OnboardingItem(
                R.drawable.three,

                "Achieve More",
                "Turn small steps into big success!"
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
