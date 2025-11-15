package com.example.moti;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private TextView initialSignin;
    private Button initialSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // get components by IDs
        initialSignin = findViewById(R.id.initial_sigin);
        initialSignup = findViewById(R.id.initial_signup);

        initialSignin.setOnClickListener( v -> {
            Intent intent = new Intent(MainActivity.this, login.class);
            startActivity(intent);
        });

        initialSignup.setOnClickListener(v-> {
           Intent intent = new Intent(MainActivity.this, register.class);
           startActivity(intent);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}