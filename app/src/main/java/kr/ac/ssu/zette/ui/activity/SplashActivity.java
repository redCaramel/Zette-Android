package kr.ac.ssu.zette.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.data.local.SettingsStore;
import kr.ac.ssu.zette.data.local.ZetteData;
import kr.ac.ssu.zette.data.local.ZetteDatabase;
import kr.ac.ssu.zette.databinding.ActivitySplashBinding;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final long MIN_SHOW_MS = 2000;
    private static final long MAX_WAIT_MS = 3000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean navigated;

    String[] splashText = new String[5];
    ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 첫 화면부터 맞는 테마로 그리기 위해 super.onCreate 전에 적용
        SettingsStore.getInstance(this).applySavedThemeMode();
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

        loadData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    // DB 열기 + 오늘 목록 미리 계산. 최소 2초는 보여주고, 3초가 지나면 끝나지 않아도 넘어간다.
    private void loadData() {
        long startedAt = SystemClock.elapsedRealtime();
        handler.postDelayed(this::goToMain, MAX_WAIT_MS);
        ZetteDatabase.IO.execute(() -> {
            try {
                ZetteData.init(getApplicationContext()).warmUp();
                long elapsed = SystemClock.elapsedRealtime() - startedAt;
                handler.postDelayed(this::goToMain, Math.max(0, MIN_SHOW_MS - elapsed));
            } catch (RuntimeException e) {
                Log.e(TAG, "데이터 준비 실패", e);
                handler.post(this::showLoadError);
            }
        });
    }

    private void goToMain() {
        if (navigated || isFinishing()) return;
        navigated = true;
        handler.removeCallbacksAndMessages(null);
        // 처음 실행(또는 데이터 초기화 후)이면 온보딩부터
        boolean onboarded = SettingsStore.getInstance(this).isOnboardingDone();
        startActivity(new Intent(this, onboarded ? MainActivity.class : OnboardingActivity.class));
        finish();
    }

    private void showLoadError() {
        if (navigated || isFinishing()) return;
        handler.removeCallbacksAndMessages(null);
        binding.loading.setVisibility(View.GONE);
        binding.textLoading.setText(R.string.splash_load_error);
    }
}