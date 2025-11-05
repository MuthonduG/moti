package com.example.moti;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> {

    private final List<OnboardingItem> onboardingItems;

    public OnboardingAdapter(List<OnboardingItem> onboardingItems) {
        this.onboardingItems = onboardingItems;
    }

    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new OnboardingViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_container_onboarding, parent, false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        holder.bind(onboardingItems.get(position));
    }

    @Override
    public int getItemCount() {
        return onboardingItems.size();
    }

    static class OnboardingViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageOnboarding;
        private final TextView textTitle;
        private final TextView textDescription;
        private final Button buttonSkip;

        OnboardingViewHolder(@NonNull View itemView) {
            super(itemView);
            imageOnboarding = itemView.findViewById(R.id.imageOnboarding);
            textTitle = itemView.findViewById(R.id.textTitle);
            textDescription = itemView.findViewById(R.id.textDescription);
            buttonSkip = itemView.findViewById(R.id.buttonSkip);

            // Skip button functionality
            buttonSkip.setOnClickListener(v -> {
                Context context = itemView.getContext();

                // Save that onboarding is complete
                SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("isFirstRun", false);
                editor.apply();

                // Go to MainActivity
                Intent intent = new Intent(context, MainActivity.class);
                context.startActivity(intent);

                // Close the onboarding activity
                if (context instanceof OnboardingActivity) {
                    ((OnboardingActivity) context).finish();
                }
            });
        }

        void bind(OnboardingItem item) {
            imageOnboarding.setImageResource(item.image);
            textTitle.setText(item.title);
            textDescription.setText(item.description);
        }
    }
}
