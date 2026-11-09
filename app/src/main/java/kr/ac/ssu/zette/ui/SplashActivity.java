package kr.ac.ssu.zette.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.databinding.ActivitySplashBinding;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    String[] splashText = new String[5];
    ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.loading.setVisibility(View.VISIBLE);

        splashText[0] = "늦지 않게, 여유 있게.";
        splashText[1] = "제때 나가면, 제때 도착해요.";
        splashText[2] = "어디서든 나갈 때를 알려드려요.";
        splashText[3] = "지금 어디 있든, 제때 출발.";
        splashText[4] = "이제 시간 계산은 제때에게.";

        int selectedIndex = (int) (Math.random() * 5);
        binding.textSplash.setText(splashText[selectedIndex]);

        // Script for Loading APP...

        binding.textLoading.setOnClickListener(view -> { // remove after writing Loading Script
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
        });

    }
}