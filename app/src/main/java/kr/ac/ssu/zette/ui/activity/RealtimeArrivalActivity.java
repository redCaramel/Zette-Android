package kr.ac.ssu.zette.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import kr.ac.ssu.zette.R;
import kr.ac.ssu.zette.data.local.ZetteData;
import kr.ac.ssu.zette.databinding.ActivityRealtimeArrivalBinding;
import kr.ac.ssu.zette.dto.Schedule;
import kr.ac.ssu.zette.dto.ScheduleKey;
import kr.ac.ssu.zette.ui.KakaoMap;
import kr.ac.ssu.zette.ui.Loading;

// 실시간 도착 (Figma 07). 홈 Hero 에서 들어온다.
// shortcut: 버스 도착 정보(도착 카드·도착 예정 목록·승차 정류장)는 샘플 문자열이다. 실시간 API 연결 시 교체
public class RealtimeArrivalActivity extends AppCompatActivity {

    private static final String EXTRA_KEY = "schedule_key";

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN);

    private ActivityRealtimeArrivalBinding binding;

    @NonNull
    public static Intent intent(@NonNull Context context, @NonNull ScheduleKey key) {
        return new Intent(context, RealtimeArrivalActivity.class).putExtra(EXTRA_KEY, key.encode());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityRealtimeArrivalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String encodedKey = getIntent().getStringExtra(EXTRA_KEY);
        if (encodedKey == null) {
            finish();
            return;
        }
        ScheduleKey key = ScheduleKey.decode(encodedKey);

        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.arrivalSkeleton.layoutSkeletonChips.setVisibility(View.GONE);
        binding.layoutArrivalLoading.tvLoading.setText(R.string.realtime_loading);
        Loading.pulse(binding.layoutLoading);
        binding.btnRefresh.setOnClickListener(v -> loadArrivals());
        loadArrivals();

        // 삭제되었거나 찾을 수 없는 일정이면 닫는다
        ZetteData.get(this).schedules.observeSchedule(key).observe(this, schedule -> {
            if (schedule == null) {
                finish();
                return;
            }
            bind(schedule);
        });
    }

    private void bind(@NonNull Schedule s) {
        binding.btnOpenMap.setEnabled(KakaoMap.canRoute(s.place));
        binding.btnOpenMap.setOnClickListener(v -> KakaoMap.openRoute(this, s.place));
        LocalDateTime departure = s.computed != null ? s.computed.expectedDeparture : null;
        binding.tvDeparture.setText(departure != null
                ? departure.toLocalTime().format(TIME_FORMAT)
                : getString(R.string.detail_sample_departure));
    }

    // 07-L: 처음엔 스켈레톤 + "실시간 도착 정보 불러오는 중…".
    // 새로고침은 보이는 값을 지우지 않고 버튼만 막는다 (로딩 표시 규칙 "다시 불러올 때")
    private void loadArrivals() {
        binding.getRoot().removeCallbacks(arrivalsLoaded);
        binding.btnRefresh.setEnabled(false);
        // TODO - api연결 구현: 실시간 버스 도착 응답 대신 3초 대기
        binding.getRoot().postDelayed(arrivalsLoaded, Loading.FAKE_DELAY_MS);
    }

    private final Runnable arrivalsLoaded = () -> {
        Loading.stopPulse(binding.layoutLoading);
        binding.layoutLoading.setVisibility(View.GONE);
        binding.layoutContent.setVisibility(View.VISIBLE);
        binding.btnRefresh.setEnabled(true);
        binding.tvUpdated.setText(getString(R.string.realtime_updated, LocalTime.now().format(TIME_FORMAT)));
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (binding != null) {
            binding.getRoot().removeCallbacks(arrivalsLoaded);
            Loading.stopPulse(binding.layoutLoading);
        }
    }
}
