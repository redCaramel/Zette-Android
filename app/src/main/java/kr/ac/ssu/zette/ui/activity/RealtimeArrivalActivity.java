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
import kr.ac.ssu.zette.ui.Network;

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
        binding.btnRefresh.setOnClickListener(v -> loadArrivals());
        binding.btnOfflineRetry.setOnClickListener(v -> loadArrivals());
        binding.networkBanner.btnRetry.setOnClickListener(v -> loadArrivals());
        // 연결되면 배너·오류 화면은 스스로 다시 불러온다
        Network.get(this).observe(this, online -> {
            boolean showingError = binding.networkBanner.getRoot().getVisibility() == View.VISIBLE
                    || binding.layoutOffline.getVisibility() == View.VISIBLE;
            if (online && showingError) loadArrivals();
        });
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
        if (!hasContent) {
            binding.layoutOffline.setVisibility(View.GONE);
            binding.scrollContent.setVisibility(View.VISIBLE);
            binding.layoutLoading.setVisibility(View.VISIBLE);
            Loading.pulse(binding.layoutLoading);
        }
        // TODO - api연결 구현: 실시간 버스 도착 응답 대신 3초 대기
        binding.getRoot().postDelayed(arrivalsLoaded, Loading.FAKE_DELAY_MS);
    }

    // 07-E: 보여줄 값이 없으면 전체 화면 오류, 이미 보여주던 값이 있으면 "몇 시 기준" 배너만 띄운다
    private boolean hasContent;
    private LocalTime lastUpdated;
    private final Runnable arrivalsLoaded = () -> {
        binding.btnRefresh.setEnabled(true);
        // TODO - api연결 구현: 실제 응답 실패(10초 초과·5xx)도 오류로 처리
        if (!Network.get(this).isOnline()) {
            if (hasContent) {
                binding.networkBanner.tvBanner.setText(
                        getString(R.string.network_unstable, lastUpdated.format(TIME_FORMAT)));
                binding.networkBanner.getRoot().setVisibility(View.VISIBLE);
            } else {
                Loading.stopPulse(binding.layoutLoading);
                binding.scrollContent.setVisibility(View.GONE);
                binding.layoutOffline.setVisibility(View.VISIBLE);
            }
            return;
        }
        hasContent = true;
        lastUpdated = LocalTime.now();
        Loading.stopPulse(binding.layoutLoading);
        binding.networkBanner.getRoot().setVisibility(View.GONE);
        binding.layoutLoading.setVisibility(View.GONE);
        binding.layoutContent.setVisibility(View.VISIBLE);
        binding.tvUpdated.setText(getString(R.string.realtime_updated, lastUpdated.format(TIME_FORMAT)));
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
